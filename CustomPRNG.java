
public final class CustomPRNG {

    // Internal state: two 64-bit values (must not both be zero)
    private long s0;
    private long s1;

    /**
     * Default constructor: seed from System.nanoTime() and identity mixing.
     */
    public CustomPRNG() {
        this(seedFromNano());
    }

    /**
     * Constructor with single long seed.
     * Uses splitmix64 to expand a single seed into two non-zero 64-bit states.
     */
    public CustomPRNG(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L; // golden ratio
        this.s0 = splitMix64(z);
        this.s1 = splitMix64(z + 0x9E3779B97F4A7C15L);
        // ensure not both zero
        if (s0 == 0 && s1 == 0) {
            s0 = 0x9E3779B97F4A7C15L;
            s1 = 0xDA3E39CB94B95BDBL;
        }
    }

    /**
     * Set seed (re-initialize state).
     * Thread-safety: if you use from multiple threads, you should synchronize externally.
     */
    public void setSeed(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        this.s0 = splitMix64(z);
        this.s1 = splitMix64(z + 0x9E3779B97F4A7C15L);
        if (s0 == 0 && s1 == 0) {
            s0 = 0x9E3779B97F4A7C15L;
            s1 = 0xDA3E39CB94B95BDBL;
        }
    }

    /**
     * splitMix64 used to generate well-mixed 64-bit values from a counter/seed.
     * This is only used for seeding, not for nextLong itself.
     */
    private static long splitMix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Generate next 64-bit pseudorandom value using xorshift128+.
     * Algorithm (reference Vigna):
     *   long x = s0;
     *   long y = s1;
     *   long result = x + y;
     *   s0 = y;
     *   x ^= x << 23;
     *   s1 = x ^ y ^ (x >>> 17) ^ (y >>> 26);
     *   return result;
     */
    public long nextLong() {
        long x = s0;
        long y = s1;
        long result = x + y;

        s0 = y;
        x ^= (x << 23); // a
        x ^= (x >>> 17); // b
        x ^= y ^ (y >>> 26); // c
        s1 = x;

        return result;
    }

    /**
     * nextInt(): return 32-bit int. Similar sematics to java.util.Random.nextInt().
     */
    public int nextInt() {
        return (int) nextLong();
    }

    /**
     * nextInt(bound): returns value in [0, bound)
     * Uses rejection sampling to avoid bias.
     */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        int r;
        int m = bound - 1;
        if ((bound & m) == 0) { // power of two
            return (int) (nextLong() & m);
        }
        // rejection sampling
        do {
            r = (int) (nextLong() >>> 1); // non-negative 31-bit
        } while (r >= Integer.MAX_VALUE - (Integer.MAX_VALUE % bound));
        return r % bound;
    }

    /**
     * nextDouble: uniform in [0.0, 1.0)
     */
    public double nextDouble() {
        // take top 53 bits of nextLong
        long bits = nextLong() >>> 11;
        return bits * (1.0 / (1L << 53));
    }

    /**
     * nextFloat: uniform in [0.0f, 1.0f)
     */
    public float nextFloat() {
        // top 24 bits
        int bits = (int) (nextLong() >>> 40);
        return bits * (1.0f / (1 << 24));
    }

    /**
     * nextBoolean
     */
    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
    }

    /**
     * Fill bytes with random values.
     */
    public void nextBytes(byte[] bytes) {
        int i = 0;
        int len = bytes.length;
        while (i < len) {
            long r = nextLong();
            for (int n = 0; n < 8 && i < len; n++) {
                bytes[i++] = (byte) (r & 0xFF);
                r >>= 8;
            }
        }
    }

    /**
     * Helper: seed from System.nanoTime() and identity mixing for default constructor.
     */
    private static long seedFromNano() {
        long t = System.nanoTime();
        t ^= Thread.currentThread().getId() << 7;
        t ^= System.identityHashCode(new Object()) << 13;
        return t;
    }

    // --- Simple demo main ---
    public static void main(String[] args) {
        CustomPRNG rnd = new CustomPRNG(123456789L);

        System.out.println("5 longs:");
        for (int i = 0; i < 5; i++) System.out.println(rnd.nextLong());

        System.out.println("\n5 ints:");
        for (int i = 0; i < 5; i++) System.out.println(rnd.nextInt());

        System.out.println("\n5 doubles:");
        for (int i = 0; i < 5; i++) System.out.println(rnd.nextDouble());

        System.out.println("\nRandom bytes (10):");
        byte[] b = new byte[10];
        rnd.nextBytes(b);
        for (byte by : b) System.out.printf("%02x ", by);
        System.out.println();
    }
}
