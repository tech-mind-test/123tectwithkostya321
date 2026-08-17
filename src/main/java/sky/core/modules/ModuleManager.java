package sky.core.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.eventapinew.EventTarget;
import sky.core.SkyCore;
import sky.core.events.EventKey;
import sky.core.utils.managers.impl.notificationmanager.NotificationManager;
import sky.core.modules.api.constructors.Setting;
import sky.core.modules.api.constructors.impl.BooleanSetting;
import sky.core.modules.impl.combat.*;
import sky.core.modules.impl.miscellaneous.*;
import sky.core.modules.impl.movement.*;
import sky.core.modules.impl.player.*;
import sky.core.modules.impl.visuals.*;
import sky.core.ui.Interface.elements.impl.NotificationRender;
import sky.core.utils.Wrapper;
import sky.core.utils.misc.SoundUtil;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;


import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ModuleManager implements Wrapper {
    private final List<Module> modules = new CopyOnWriteArrayList<>();
    public AttackAura attackAura;
    public CrystalAura crystalAura;
    public AntiThorns antiThorns;
    public SwordAnimations swordAnimations;
    public ViewModel viewModel;
    public Removals removals;
    public AspectRatio aspectRatio;
    public Ambience ambience;
    public ElytraTarget elytraTarget;
    public NoWeb noWeb;
    public ElytraMotion elytraMotion;
    public OpenWalls openWalls;
    public SantaHat santaHat;
    public ChatHelper chatHelper;
    public TargetESP targetESP;

    public void init() {


        this.attackAura = new AttackAura();
        add(attackAura);
        add(new AutoTotem());
        add(new AntiBot());
        add(new AutoExplosion());
        this.crystalAura = new CrystalAura();
        add(crystalAura);
        add(new InstantRebreak());
        add(new HolyWorldHelper());
        add(new NoFriendDamage());
        add(new AutoSwap());
        add(new AimBot());
        add(new NoEntityTrace());
        add(new HitBox());
        add(new NoSlotChange());
        add(new NoVelocity());
        add(new PacketCriticals());
        add(new ItemRelease());
        this.antiThorns = new AntiThorns();
        add(antiThorns);


        // Movement
        add(new Sprint());
        add(new NoSlow());
        add(new ElytraBooster());
        add(new FreeCamera());
        add(new AirStuck());
        add(new NoJumpDelay());

        add(new Timer());
        add(new NoPush());
        add(new GuiMove());
        add(new Speed());
        this.noWeb = new NoWeb();
        add(noWeb);
        add(new NoControllerWeb());
        add(new Flight());
        this.elytraMotion = new ElytraMotion();
        add(elytraMotion);


        add(new ClickPearl());
        add(new AutoRespawn());
        add(new ItemsCooldown());
        add(new FastExp());
        add(new ItemScroller());

        add(new AutoPotion());
        add(new AimingItems());
        add(new AutoTool());
        add(new AntiAFK());

        add(new LockSlot());
        add(new FastBreak());
        add(new AutoArmor());
        add(new AutoLeave());
        add(new AutoJoiner());
        add(new ChestStealer());
        add(new NoInteract());

        this.ambience = new Ambience();
        add(ambience);
        add(new GhostPlayer());
        add(new Spider());
        this.viewModel = new ViewModel();
        add(viewModel);
        add(new Box3D());
        this.targetESP = new TargetESP();
        add(targetESP);
        add(new BlockOutline());
        this.swordAnimations = new SwordAnimations();
        add(swordAnimations);
        add(new ItemReplacer());
        this.removals = new Removals();
        add(removals);
        this.aspectRatio = new AspectRatio();
        add(new KillEffect());
        add(aspectRatio);
        add(new ArmorDurabilityView());
        add(new ExtendedTab());
        add(new ItemPhysics());
        add(new ChinaHat());
        add(new Prediction());
        add(new Tags());
        add(new Arrows());
        add(new SRPSpoofer());
        add(new FullBright());
        add(new Saturation());
        add(new Interface());
        add(new Tracers());
        add(new Trails());
        add(new SeeInvisibles());
        add(new ShulkerPreview());
        add(new Hands());
        add(new Animation());
        add(new FireworkESP());
        add(new Crosshair());
        add(new CrosshairAddons());
        add(new Wings());
        add(new CustomModels());
        add(new RichiDog());


        // Miscellaneous
        add(new ElytraHelper());
        add(new AutoAccept());
        add(new AutoMessage());
        add(new ItemHelper());
        add(new ClickFriend());
        add(new NameProtect());
        add(new TargetPearl());
        add(new Cubes());
        add(new PotionTracker());

        add(new UseTracker());
        add(new ToggleSounds());
        this.openWalls = new OpenWalls();
        add(this.openWalls);
        add(new GrimGlide());
        add(new ScoreboardHealth());
        add(new ReallyWorldHelper());
        add(new TapeMouse());
        add(new AutoContract());
        add(new AutoDuel());
        this.chatHelper = new ChatHelper();
        add(this.chatHelper);
        add(new BlockESP());
        this.elytraTarget = new ElytraTarget();
        add(elytraTarget);
        add(new Particles());
        add(new ParticularWater());
        add(new TotemParticles());
        add(new JumpCircle());

        modules.sort(Comparator.comparing(Module::getName));
        EventManager.register(this);
    }

    private void add(Module module) {
        modules.add(module);
    }

    public Module getModule(Class<? extends Module> classModule) {
        for (Module module : modules) {
            if (module != null && module.getClass() == classModule) {
                return module;
            }
        }
        return null;
    }

    public Optional<Module> findModuleByName(String name) {
        return modules.stream().filter(module -> module.getName().equalsIgnoreCase(name)).findFirst();
    }

    @EventTarget
    private void onKey(EventKey e) {
        if (e.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            mc.displayGuiScreen(SkyCore.getInstance().getDropDown());
        }
        for (Module module : modules) {
            if (module.getBind() == e.getKey()) {
                if ((module.getToggleMode() == Module.ToggleMode.TOGGLE && e.isHold()) || module.getToggleMode() == Module.ToggleMode.HOLD) {
                    module.toggle();
                }
            }
            for (Setting<?> setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getBind() == e.getKey()) {
                    boolean isToggleMode = booleanSetting.getToggleMode() == Module.ToggleMode.TOGGLE;
                    if (!isToggleMode || e.isHold()) {
                        boolean newValue = !booleanSetting.get();
                        booleanSetting.set(newValue);
                        if (!Module.isSuppressToggleEffects() && booleanSetting.isKeybindvisible()) {
                            SoundUtil.playSound(ToggleSounds.getSoundFile(newValue));
                            if (NotificationRender.module.get()) {
                                String categoryIcon = NotificationRender.getCategoryIcon(module);
                                NotificationManager.addNotification(categoryIcon, "Module " + booleanSetting.getName() + (newValue ? " enabled!" : " disabled!"), -1);
                            }
                        }
                    }
                }
            }
        }
    }
}
