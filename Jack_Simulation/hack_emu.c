#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>

#define RAM_SIZE 65536
#define HALT_ADDR 30001 // Sys.fail writes error code here
#define PASS_ADDR 30000 // Sys.halt / Main writes PASS flag here
#define SCREEN_BASE 16384
#define KBD_ADDR 24576

typedef struct
{
    uint16_t *rom;
    int rom_len;

    uint16_t ram[RAM_SIZE];

    uint16_t A;
    uint16_t D;
    int pc;

    unsigned long long instr_count;
} CPU;

/*---------------- Utility: Load .hack file ----------------*/

static uint16_t *load_hack_file(const char *filename, int *out_len)
{
    FILE *fp = fopen(filename, "r");
    if (!fp)
    {
        perror("fopen");
        return NULL;
    }

    int capacity = 4096;
    uint16_t *rom = (uint16_t *)malloc(sizeof(uint16_t) * capacity);
    if (!rom)
    {
        fclose(fp);
        fprintf(stderr, "malloc failed\n");
        return NULL;
    }

    char buf[256];
    int count = 0;

    while (fgets(buf, sizeof(buf), fp))
    {
        char *p = buf;
        // Skip whitespace
        while (*p == ' ' || *p == '\t')
            p++;

        // Skip empty lines and comments
        if (*p == '\0' || *p == '\n' || *p == '\r' || *p == '/' || *p == '#')
        {
            continue;
        }

        // Parse binary string
        uint16_t v = 0;
        int bit_count = 0;
        for (; *p == '0' || *p == '1'; p++)
        {
            v <<= 1;
            if (*p == '1')
                v |= 1;
            bit_count++;
        }

        // Ensure at least 1 bit was read
        if (bit_count == 0)
        {
            continue;
        }

        // Expand capacity if needed
        if (count >= capacity)
        {
            capacity *= 2;
            uint16_t *new_rom = (uint16_t *)realloc(rom, sizeof(uint16_t) * capacity);
            if (!new_rom)
            {
                fclose(fp);
                free(rom);
                fprintf(stderr, "realloc failed\n");
                return NULL;
            }
            rom = new_rom;
        }
        rom[count++] = v;
    }

    fclose(fp);
    *out_len = count;
    return rom;
}

/*---------------- ALU ----------------*/

static inline int alu_compute(uint8_t comp7, int x, int y)
{
    switch (comp7)
    {
    /* a = 0 series */
    case 0b0101010:
        return 0; // 0
    case 0b0111111:
        return 1; // 1
    case 0b0111010:
        return -1; // -1
    case 0b0001100:
        return x; // D
    case 0b0110000:
        return y; // A
    case 0b0001101:
        return ~x; // !D
    case 0b0110001:
        return ~y; // !A
    case 0b0001111:
        return -x; // -D
    case 0b0110011:
        return -y; // -A
    case 0b0011111:
        return x + 1; // D+1
    case 0b0110111:
        return y + 1; // A+1
    case 0b0001110:
        return x - 1; // D-1
    case 0b0110010:
        return y - 1; // A-1
    case 0b0000010:
        return x + y; // D+A
    case 0b0010011:
        return x - y; // D-A
    case 0b0000111:
        return y - x; // A-D
    case 0b0000000:
        return x & y; // D&A
    case 0b0010101:
        return x | y; // D|A

    /* a = 1 series (M) */
    case 0b1110000:
        return y; // M
    case 0b1110001:
        return ~y; // !M
    case 0b1110011:
        return -y; // -M
    case 0b1110111:
        return y + 1; // M+1
    case 0b1110010:
        return y - 1; // M-1
    case 0b1000010:
        return x + y; // D+M
    case 0b1010011:
        return x - y; // D-M
    case 0b1000111:
        return y - x; // M-D
    case 0b1000000:
        return x & y; // D&M
    case 0b1010101:
        return x | y; // D|M

    default:
        return 0;
    }
}

static inline int should_jump(uint8_t jump, int zr, int ng)
{
    int gt = (!ng && !zr); // > 0
    int lt = (ng);         // < 0
    int eq = (zr);         // == 0

    switch (jump)
    {
    case 0b000:
        return 0; // null
    case 0b001:
        return gt; // JGT
    case 0b010:
        return eq; // JEQ
    case 0b011:
        return gt || eq; // JGE
    case 0b100:
        return lt; // JLT
    case 0b101:
        return !eq; // JNE
    case 0b110:
        return lt || eq; // JLE
    case 0b111:
        return 1; // JMP
    default:
        return 0;
    }
}

/*---------------- Runtime init (registers + RAM) ----------------*/

// レジスタと RAM 全体（= ヒープ含む）をクリア
static void cpu_reset(CPU *cpu)
{
    cpu->A = 0;
    cpu->D = 0;
    cpu->pc = 0;
    cpu->instr_count = 0ULL;

    for (int i = 0; i < RAM_SIZE; i++)
    {
        cpu->ram[i] = 0;
    }
}

/*---------------- CPU Implementation ----------------*/

static void cpu_init(CPU *cpu, uint16_t *rom, int rom_len)
{
    // Clear entire structure for reliable initialization
    memset(cpu, 0, sizeof(CPU));

    cpu->rom = rom;
    cpu->rom_len = rom_len;

    // 初期起動時にもレジスタ + RAM をクリア
    cpu_reset(cpu);
}

/*
 * Main execution loop
 *  - trace_fp == NULL: no trace output (fastest)
 *  - trace_fp != NULL: log PC at startup + periodic intervals
 *
 * ★ここで毎回 cpu_reset を呼ぶので、
 *   ./hack_emu Prog.hack ... を実行するたびに
 *   RAM/ヒープは必ずゼロからスタートする
 */
static void cpu_run(CPU *cpu, unsigned long long max_steps, FILE *trace_fp)
{
    // 実行開始のたびにレジスタ + RAM（＝ヒープ含む）を初期化
    cpu_reset(cpu);

    unsigned long long cnt = 0;
    const int rom_len = cpu->rom_len;
    uint16_t *ram = cpu->ram;
    uint16_t *rom = cpu->rom;
    int pc = cpu->pc;
    uint16_t A = cpu->A;
    uint16_t D = cpu->D;

    if (trace_fp)
    {
        fprintf(trace_fp, "=== CPU Execution Started ===\n");
        fprintf(trace_fp, "ROM size: %d instructions\n", rom_len);
        fprintf(trace_fp, "Max steps: %llu\n", max_steps);
        fprintf(trace_fp, "Initial state: PC=%d A=%u D=%u\n\n", pc, A, D);
    }

    while (1)
    {
        /* ----------------- Trace output ----------------- */
        if (trace_fp)
        {
            if (cnt < 200 || (cnt % 1000000ULL) == 0ULL)
            {
                fprintf(trace_fp,
                        "TRACE cnt=%llu PC=%d A=%u D=%u RAM[HALT]=%u RAM[PASS]=%u\n",
                        cnt, pc, (unsigned)A, (unsigned)D,
                        (unsigned)ram[HALT_ADDR],
                        (unsigned)ram[PASS_ADDR]);
            }
        }

        // 1. Check HALT flag (abnormal termination)
        if (ram[HALT_ADDR] != 0)
        {
            if (trace_fp)
            {
                fprintf(trace_fp,
                        "\n>>> HALT FLAG DETECTED <<<\n");
                fprintf(trace_fp,
                        "  Count: %llu\n", cnt);
                fprintf(trace_fp,
                        "  PC: %d\n", pc);
                fprintf(trace_fp,
                        "  RAM[%d] (HALT): %u (0x%04X)\n",
                        HALT_ADDR, (unsigned)ram[HALT_ADDR], (unsigned)ram[HALT_ADDR]);
                fprintf(trace_fp,
                        "  RAM[%d] (PASS): %u (0x%04X)\n",
                        PASS_ADDR, (unsigned)ram[PASS_ADDR], (unsigned)ram[PASS_ADDR]);
            }
            break;
        }

        // 1b. Check PASS flag (normal termination)
        if (ram[PASS_ADDR] != 0)
        {
            if (trace_fp)
            {
                fprintf(trace_fp,
                        "\n>>> PASS FLAG DETECTED <<<\n");
                fprintf(trace_fp,
                        "  Count: %llu\n", cnt);
                fprintf(trace_fp,
                        "  PC: %d\n", pc);
                fprintf(trace_fp,
                        "  RAM[%d] (PASS): %u (0x%04X)\n",
                        PASS_ADDR, (unsigned)ram[PASS_ADDR], (unsigned)ram[PASS_ADDR]);
            }
            break;
        }

        // 2. PC range check
        if (pc < 0 || pc >= rom_len)
        {
            if (trace_fp)
            {
                fprintf(trace_fp,
                        "\n>>> PC OUT OF RANGE <<<\n");
                fprintf(trace_fp,
                        "  PC: %d (valid range: 0-%d)\n", pc, rom_len - 1);
                fprintf(trace_fp,
                        "  Count: %llu\n", cnt);
                fprintf(trace_fp,
                        "  RAM[%d] (PASS): %u\n",
                        PASS_ADDR, (unsigned)ram[PASS_ADDR]);
            }
            break;
        }

        // 3. Safety limit
        if (cnt >= max_steps)
        {
            if (trace_fp)
            {
                fprintf(trace_fp,
                        "\n>>> MAX STEPS REACHED <<<\n");
                fprintf(trace_fp,
                        "  Count: %llu\n", cnt);
                fprintf(trace_fp,
                        "  PC: %d\n", pc);
                fprintf(trace_fp,
                        "  RAM[%d] (PASS): %u\n",
                        PASS_ADDR, (unsigned)ram[PASS_ADDR]);
            }
            break;
        }

        // ---- 4. Execute one instruction ----
        uint16_t instr = rom[pc];

        // Determine A-instruction / C-instruction
        if ((instr & 0x8000) == 0)
        {
            // A-instruction
            A = instr & 0x7FFF;
            pc = pc + 1;
        }
        else if ((instr & 0xE000) == 0xE000)
        {
            // C-instruction
            uint8_t comp7 = (instr >> 6) & 0x7F;
            uint8_t dest = (instr >> 3) & 0x07;
            uint8_t jump = instr & 0x07;

            int aBit = (comp7 >> 6) & 0x1;
            int x = (int)D;
            int y = (aBit == 0) ? (int)A : (int)ram[A];

            int out = alu_compute(comp7, x, y) & 0xFFFF;
            int zr = (out == 0);
            int ng = (out & 0x8000) != 0;

            uint16_t oldA = A;

            // Write to M
            if (dest & 0b001)
            {
                ram[oldA] = (uint16_t)out;

                // Log writes to HALT_ADDR / PASS_ADDR
                if (trace_fp && (oldA == HALT_ADDR || oldA == PASS_ADDR))
                {
                    fprintf(trace_fp,
                            ">>> WRITE RAM[%d] = %u (0x%04X) at cnt=%llu, PC=%d, instr=0x%04X\n",
                            oldA, (unsigned)out, (unsigned)out, cnt, pc, instr);
                }
            }

            // Update A, D registers
            if (dest & 0b100)
            {
                A = (uint16_t)out;
            }
            if (dest & 0b010)
            {
                D = (uint16_t)out;
            }

            int next_pc = pc + 1;
            if (should_jump(jump, zr, ng))
            {
                next_pc = (int)A;
            }
            pc = next_pc;
        }
        else
        {
            if (trace_fp)
            {
                fprintf(trace_fp,
                        "\n>>> ILLEGAL INSTRUCTION <<<\n");
                fprintf(trace_fp,
                        "  Instruction: 0x%04X\n", instr);
                fprintf(trace_fp,
                        "  PC: %d\n", pc);
                fprintf(trace_fp,
                        "  Count: %llu\n", cnt);
                fprintf(trace_fp,
                        "  RAM[%d] (PASS): %u\n",
                        PASS_ADDR, (unsigned)ram[PASS_ADDR]);
            }
            break;
        }

        cnt++;
    }

    // Write final state back to structure
    cpu->pc = pc;
    cpu->A = A;
    cpu->D = D;
    cpu->instr_count = cnt;

    if (trace_fp)
    {
        fprintf(trace_fp, "\n=== CPU Execution Finished ===\n");
        fprintf(trace_fp, "Final state: PC=%d A=%u D=%u\n", pc, A, D);
        fprintf(trace_fp, "Total instructions executed: %llu\n", cnt);
    }
}

/*---------------- Dump functions ----------------*/

static void dump_ram_range(CPU *cpu, int start, int end, const char *label)
{
    printf("\n%s [%d..%d]:\n", label, start, end);
    for (int addr = start; addr <= end && addr < RAM_SIZE; addr++)
    {
        uint16_t v = cpu->ram[addr];
        // ← ここの if (v != 0) を削除
        printf("  RAM[%d] = %u (0x%04X) [0b", addr, (unsigned)v, (unsigned)v);
        for (int bit = 15; bit >= 0; bit--)
        {
            printf("%d", (v >> bit) & 1);
            if (bit == 8)
                printf(" ");
        }
        printf("]\n");
    }
}

/*---------------- main ----------------*/

int main(int argc, char **argv)
{
    // --- 変更ここから ---
    const char *filename;
    unsigned long long max_steps = ~0ULL;
    const char *trace_filename = NULL;

    if (argc < 2)
    {
        // 引数がない場合のデフォルトパス（相対パス）
        filename = "../bin/Prog.hack";
        printf("[INFO] No file specified. Using default: %s\n", filename);
    }
    else
    {
        filename = argv[1];
    }

    if (argc >= 3)
    {
        max_steps = strtoull(argv[2], NULL, 10);
    }
    if (argc >= 4)
    {
        trace_filename = argv[3];
    }

    printf("===========================================\n");
    printf("  Hack CPU Emulator (Improved)\n");
    printf("===========================================\n\n");

    // Load ROM
    int rom_len = 0;
    uint16_t *rom = load_hack_file(filename, &rom_len);
    if (!rom)
    {
        fprintf(stderr, "ERROR: Failed to load %s\n", filename);
        return 1;
    }

    printf("Loading: %s\n", filename);
    printf("Loaded ROM size: %d instructions\n", rom_len);
    if (max_steps != ~0ULL)
    {
        printf("Max steps: %llu\n", max_steps);
    }
    if (trace_filename)
    {
        printf("Trace output: %s\n", trace_filename);
    }
    printf("\n");

    // Initialize CPU
    CPU cpu;
    cpu_init(&cpu, rom, rom_len);

    // Open trace file
    FILE *trace_fp = NULL;
    if (trace_filename)
    {
        trace_fp = fopen(trace_filename, "w");
        if (!trace_fp)
        {
            perror("ERROR: fopen traceFile");
            free(rom);
            return 1;
        }
    }

    printf("Starting execution...\n");
    clock_t start = clock();

    // Execute
    cpu_run(&cpu, max_steps, trace_fp);

    clock_t end = clock();
    double elapsed = (double)(end - start) / CLOCKS_PER_SEC;

    if (trace_fp)
    {
        fclose(trace_fp);
    }

    // Display execution results
    printf("\n===========================================\n");
    printf("  Execution Results\n");
    printf("===========================================\n");
    printf("Executed instructions: %llu\n", cpu.instr_count);
    printf("Elapsed time: %.6f sec\n", elapsed);
    if (cpu.instr_count > 0 && elapsed > 0)
    {
        // printf("Speed: %.2f M instructions/sec\n",
        //        (cpu.instr_count / elapsed) / 1000000.0);
    }
    printf("\nFinal CPU State:\n");
    printf("  PC: %d\n", cpu.pc);
    printf("  A:  %u (0x%04X)\n", (unsigned)cpu.A, (unsigned)cpu.A);
    printf("  D:  %u (0x%04X)\n", (unsigned)cpu.D, (unsigned)cpu.D);

    /* Determine PASS / HALT flags */
    uint16_t pass = cpu.ram[PASS_ADDR];
    uint16_t halt = cpu.ram[HALT_ADDR];

    printf("\nTest Status:\n");
    printf("  PASS FLAG RAM[%d] = %u (0x%04X)\n",
           PASS_ADDR, (unsigned)pass, (unsigned)pass);
    printf("  HALT FLAG RAM[%d] = %u (0x%04X)\n",
           HALT_ADDR, (unsigned)halt, (unsigned)halt);

    printf("\n");
    if (halt != 0)
    {
        printf("=> TEST FAILED (error code = %u)\n", (unsigned)halt);
    }
    else if (pass == 0xFFFF)
    {
        printf("=> ALL TESTS PASSED\n");
    }
    else
    {
        printf("=> UNKNOWN STATE (no HALT, PASS flag not -1)\n");
    }

    /* Dump relevant RAM regions */
    dump_ram_range(&cpu, HALT_ADDR, HALT_ADDR + 500, "DEBUG AREA");

    printf("\n===========================================\n");

    free(rom);
    return 0;
}
