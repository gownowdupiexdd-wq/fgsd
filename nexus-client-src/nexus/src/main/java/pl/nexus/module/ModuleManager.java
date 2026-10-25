package pl.nexus.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import pl.nexus.Config;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static void reg(Module m) { ALL.add(m); }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    public static boolean isOn(String name) {
        for (Module m : ALL) if (m.name.equals(name)) return m.enabled;
        return false;
    }

    public static void tick() {
        if (mc.player == null || mc.world == null) return;
        for (Module m : ALL) if (m.enabled) m.onTick();
    }

    private static Entity nearest(double range, boolean playersOnly) {
        PlayerEntity p = mc.player;
        Entity best = null;
        double bd = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == p || !le.isAlive() || e instanceof ArmorStandEntity) continue;
            if (playersOnly && !(e instanceof PlayerEntity)) continue;
            if (e instanceof PlayerEntity pe && Config.friends.contains(pe.getName().getString().toLowerCase())) continue;
            double d = p.squaredDistanceTo(e);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    private static boolean moving() {
        var o = mc.options;
        return o.forwardKey.isPressed() || o.backKey.isPressed() || o.leftKey.isPressed() || o.rightKey.isPressed();
    }

    static {
        // ---------------- COMBAT ----------------
        reg(new Module("KillAura", "Automatycznie atakuje najbliższe moby i graczy", Category.COMBAT) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.getAttackCooldownProgress(0.5f) < 1f) return;
                Entity t = nearest(value, false);
                if (t == null) return;
                face(t);
                mc.interactionManager.attackEntity(p, t);
                p.swingHand(Hand.MAIN_HAND);
            }
        }.withValue("Zasięg", 4.2, 2.0, 6.0));

        reg(new Module("TriggerBot", "Bije gdy celownik jest na bycie", Category.COMBAT) {
            @Override public void onTick() {
                var p = mc.player;
                if (mc.crosshairTarget instanceof EntityHitResult r && r.getEntity() instanceof LivingEntity
                        && !(r.getEntity() instanceof PlayerEntity pe && Config.friends.contains(pe.getName().getString().toLowerCase()))
                        && p.getAttackCooldownProgress(0.5f) >= 1f) {
                    mc.interactionManager.attackEntity(p, r.getEntity());
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        });

        reg(new Module("AimBot", "Celuje w najbliższego gracza", Category.COMBAT) {
            @Override public void onTick() {
                Entity t = nearest(value, true);
                if (t != null) face(t);
            }
        }.withValue("Zasięg", 5.0, 2.0, 12.0));

        // ---------------- MOVEMENT ----------------
        reg(new Module("Fly", "Latanie jak w trybie kreatywnym", Category.MOVEMENT) {
            @Override public void onTick() {
                var a = mc.player.getAbilities();
                a.allowFlying = true; a.flying = true;
                a.setFlySpeed((float) (value * 0.05));
            }
            @Override public void onDisable() {
                if (mc.player == null) return;
                var a = mc.player.getAbilities();
                a.setFlySpeed(0.05f);
                if (!mc.player.isCreative() && !mc.player.isSpectator()) { a.allowFlying = false; a.flying = false; }
            }
        }.withValue("Prędkość", 2.0, 0.5, 8.0));

        reg(new Module("Speed", "Szybsze bieganie po ziemi", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player; var o = mc.options;
                if (!p.isOnGround() || !moving()) return;
                int fwd = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
                int str = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
                double yaw = Math.toRadians(p.getYaw());
                double x = -Math.sin(yaw) * fwd + Math.cos(yaw) * str;
                double z = Math.cos(yaw) * fwd + Math.sin(yaw) * str;
                double len = Math.sqrt(x * x + z * z);
                if (len == 0) return;
                double s = value * 0.22;
                p.setVelocity(x / len * s, p.getVelocity().y, z / len * s);
            }
        }.withValue("Mnożnik", 1.4, 1.0, 3.0));

        reg(new Module("Sprint", "Zawsze sprintuj", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player;
                if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                        && p.getHungerManager().getFoodLevel() > 6) p.setSprinting(true);
            }
        });

        reg(new Module("Step", "Wchodzisz na bloki bez skakania", Category.MOVEMENT) {
            @Override public void onTick() {
                var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
                if (attr != null) attr.setBaseValue(value);
            }
            @Override public void onDisable() {
                if (mc.player == null) return;
                var attr = mc.player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
                if (attr != null) attr.setBaseValue(0.6);
            }
        }.withValue("Wysokość", 1.0, 0.6, 2.5));

        reg(new Module("Glide", "Powolne opadanie w powietrzu", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player; var v = p.getVelocity();
                if (!p.isOnGround() && !p.isTouchingWater() && v.y < -0.1) p.setVelocity(v.x, -0.1, v.z);
            }
        });

        // ---------------- RENDER ----------------
        reg(new Module("Fullbright", "Pełna jasność (noktowizja)", Category.RENDER) {
            @Override public void onTick() {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
            }
            @Override public void onDisable() {
                if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            }
        });
        reg(new Module("HUD", "Koordynaty i FPS na ekranie", Category.RENDER) {}.on());
        reg(new Module("ArrayList", "Lista włączonych modułów", Category.RENDER) {}.on());

        // ---------------- PLAYER ----------------
        reg(new Module("NoFall", "Brak obrażeń od upadku", Category.PLAYER) {
            @Override public void onTick() {
                var p = mc.player;
                if (p.fallDistance > 2.0 && mc.getNetworkHandler() != null)
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            }
        });
        reg(new Module("AutoRespawn", "Automatyczny respawn po śmierci", Category.PLAYER) {
            @Override public void onTick() { if (mc.player.isDead()) mc.player.requestRespawn(); }
        });
        reg(new Module("AntiAFK", "Ruch ręką co jakiś czas", Category.PLAYER) {
            int t;
            @Override public void onTick() {
                if (++t >= value * 20) { t = 0; mc.player.swingHand(Hand.MAIN_HAND); mc.player.jump(); }
            }
        }.withValue("Sekundy", 20, 5, 60));
        reg(new Module("Spin", "Kręcisz się jak bączek (bajer)", Category.PLAYER) {
            @Override public void onTick() { mc.player.setYaw(mc.player.getYaw() + (float) value); }
        }.withValue("Prędkość", 20, 2, 60));

        // ================= NOWE: COMBAT =================
        reg(new Module("AutoTotem", "Totem automatycznie do drugiej ręki", Category.COMBAT) {
            @Override public void onTick() {
                var p = mc.player;
                if (mc.currentScreen != null || p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
                for (int i = 9; i <= 44; i++) {
                    if (p.playerScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, i, 40, SlotActionType.SWAP, p);
                        return;
                    }
                }
            }
        });

        reg(new Module("AutoEat", "Je jedzenie z paska gdy głód spadnie", Category.COMBAT) {
            boolean eating;
            private boolean badFood(ItemStack s) {
                return s.isOf(Items.ROTTEN_FLESH) || s.isOf(Items.SPIDER_EYE) || s.isOf(Items.POISONOUS_POTATO)
                        || s.isOf(Items.PUFFERFISH) || s.isOf(Items.CHORUS_FRUIT);
            }
            @Override public void onTick() {
                var p = mc.player;
                boolean need = p.getHungerManager().getFoodLevel() <= value && p.getHungerManager().isNotFull();
                if (need) {
                    int slot = -1;
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = p.getInventory().getStack(i);
                        if (s.contains(DataComponentTypes.FOOD) && !badFood(s)) { slot = i; break; }
                    }
                    if (slot >= 0) {
                        p.getInventory().selectedSlot = slot;
                        mc.options.useKey.setPressed(true);
                        eating = true;
                        return;
                    }
                }
                if (eating) { mc.options.useKey.setPressed(false); eating = false; }
            }
            @Override public void onDisable() { if (eating) { mc.options.useKey.setPressed(false); eating = false; } }
        }.withValue("Głód ≤", 14, 4, 19));

        // ================= NOWE: MOVEMENT =================
        reg(new Module("Jesus", "Chodzenie po wodzie", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player; Vec3d v = p.getVelocity();
                if (p.isTouchingWater() && !p.isSneaking()) p.setVelocity(v.x, 0.12, v.z);
            }
        });

        reg(new Module("HighJump", "Wyższy skok", Category.MOVEMENT) {
            boolean was;
            @Override public void onTick() {
                var p = mc.player; Vec3d v = p.getVelocity();
                boolean g = p.isOnGround();
                if (was && !g && v.y > 0.2 && mc.options.jumpKey.isPressed()) p.setVelocity(v.x, v.y * value, v.z);
                was = g;
            }
        }.withValue("Mnożnik", 1.8, 1.2, 4.0));

        reg(new Module("InfiniteJump", "Skacz w powietrzu bez końca", Category.MOVEMENT) {
            int cd;
            @Override public void onTick() {
                var p = mc.player; Vec3d v = p.getVelocity();
                if (cd > 0) cd--;
                if (mc.options.jumpKey.isPressed() && !p.isOnGround() && cd == 0 && !p.isTouchingWater()) {
                    p.setVelocity(v.x, 0.42, v.z); cd = 6;
                }
            }
        });

        reg(new Module("Spider", "Wspinanie po ścianach", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player; Vec3d v = p.getVelocity();
                if (p.horizontalCollision && mc.options.forwardKey.isPressed()) p.setVelocity(v.x, 0.2, v.z);
            }
        });

        reg(new Module("FastClimb", "Szybsze wchodzenie po drabinach", Category.MOVEMENT) {
            @Override public void onTick() {
                var p = mc.player; Vec3d v = p.getVelocity();
                if (p.isClimbing() && (p.horizontalCollision || mc.options.jumpKey.isPressed())) p.setVelocity(v.x, 0.2 * value, v.z);
            }
        }.withValue("Mnożnik", 1.5, 1.0, 3.0));

        // ================= NOWE: PLAYER =================
        reg(new Module("AutoWalk", "Automatycznie idziesz do przodu", Category.PLAYER) {
            @Override public void onTick() { mc.options.forwardKey.setPressed(true); }
            @Override public void onDisable() { mc.options.forwardKey.setPressed(false); }
        });

        // ================= NOWE: WORLD =================
        reg(new Module("Nuker", "Niszczy bloki wokół ciebie", Category.WORLD) {
            @Override public void onTick() {
                var p = mc.player;
                int r = (int) value;
                BlockPos pp = p.getBlockPos(), best = null;
                double bd = Double.MAX_VALUE;
                for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
                    BlockPos pos = pp.add(x, y, z);
                    BlockState st = mc.world.getBlockState(pos);
                    if (st.isAir() || !st.getFluidState().isEmpty() || st.getHardness(mc.world, pos) < 0) continue;
                    double d = pos.toCenterPos().squaredDistanceTo(p.getEyePos());
                    if (d <= value * value && d < bd) { bd = d; best = pos; }
                }
                if (best != null) {
                    mc.interactionManager.updateBlockBreakingProgress(best, Direction.UP);
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        }.withValue("Zasięg", 4, 1, 6));

        reg(new Module("Scaffold", "Stawia bloki pod twoimi nogami", Category.WORLD) {
            @Override public void onTick() {
                var p = mc.player;
                BlockPos below = p.getBlockPos().down();
                if (!mc.world.getBlockState(below).isReplaceable()) return;
                int slot = -1;
                for (int i = 0; i < 9; i++) if (p.getInventory().getStack(i).getItem() instanceof BlockItem) { slot = i; break; }
                if (slot < 0) return;
                for (Direction d : Direction.values()) {
                    if (d == Direction.UP) continue;
                    BlockPos nb = below.offset(d);
                    if (mc.world.getBlockState(nb).isReplaceable()) continue;
                    p.getInventory().selectedSlot = slot;
                    Direction face = d.getOpposite();
                    Vec3d hitPos = Vec3d.ofCenter(nb).add(face.getOffsetX() * 0.5, face.getOffsetY() * 0.5, face.getOffsetZ() * 0.5);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, new BlockHitResult(hitPos, face, nb, false));
                    p.swingHand(Hand.MAIN_HAND);
                    return;
                }
            }
        });
    }

    private ModuleManager() {}
}
