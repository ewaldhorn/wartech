package wartech.combat;

// Visual particle effects, explosions, muzzle flashes, and salvaging beams

public final class Effect {
    public static final byte TYPE_EXPLOSION = 0;
    public static final byte TYPE_MUZZLE = 1;
    public static final byte TYPE_SPARK = 2;
    public static final byte TYPE_HARVEST_DUST = 3;
    public static final byte TYPE_HEAL = 4;
    public static final byte TYPE_ORDER_MOVE = 5;
    public static final byte TYPE_ORDER_ATTACK = 6;

    public byte type;
    public double x;
    public double y;
    public double timer = 0.0;
    public double maxDuration;
    public double radius;
    public boolean active = true;

    public Effect(byte type, double x, double y, double duration, double radius) {
        init(type, x, y, duration, radius);
    }

    public void init(byte type, double x, double y, double duration, double radius) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.timer = 0.0;
        this.maxDuration = duration;
        this.radius = radius;
        this.active = true;
    }

    public void update(double dt) {
        timer += dt;
        if (timer >= maxDuration) {
            active = false;
        }
    }
}
