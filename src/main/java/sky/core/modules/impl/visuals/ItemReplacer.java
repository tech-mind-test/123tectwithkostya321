package sky.core.modules.impl.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import sky.core.modules.Category;
import sky.core.modules.Module;
import sky.core.modules.api.constructors.impl.ModeSetting;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ItemReplacer extends Module {
    private static final SwordVariant[] SWORD_VARIANTS = createSwordVariants();
    private static final Map<String, SwordVariant> VARIANTS_BY_LABEL = createVariantMap();

    private static ItemReplacer instance;

    private final ModeSetting swordModel =
            new ModeSetting("Модель меча", SWORD_VARIANTS[0].label, getVariantLabels());

    private String cachedSelectedLabel;
    private SwordVariant cachedSelectedVariant = SWORD_VARIANTS[0];

    public ItemReplacer() {
        super("ItemReplacer", "Заменяет модели мечей", Category.Visuals);
        instance = this;
        addSettings(swordModel);
    }

    public static boolean renderReplacement(
            ItemStack stack,
            ItemCameraTransforms.TransformType transformType,
            MatrixStack matrixStack,
            IRenderTypeBuffer buffer,
            int combinedLight,
            int combinedOverlay
    ) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof SwordItem)) {
            return false;
        }

        ItemReplacer replacer = instance;
        if (replacer == null || !replacer.isEnabled()) {
            return false;
        }

        return replacer.getSelectedVariant().model.render(
                stack,
                transformType,
                matrixStack,
                buffer,
                combinedLight,
                combinedOverlay
        );
    }

    public SwordVariant getSelectedVariant() {
        String selected = swordModel.get();
        if (selected == null) {
            return SWORD_VARIANTS[0];
        }

        if (selected.equalsIgnoreCase(cachedSelectedLabel)) {
            return cachedSelectedVariant;
        }

        SwordVariant variant = VARIANTS_BY_LABEL.get(selected.toLowerCase(Locale.ROOT));
        if (variant == null) {
            variant = SWORD_VARIANTS[0];
        }

        cachedSelectedLabel = selected;
        cachedSelectedVariant = variant;
        return variant;
    }

    @Override
    public void onDisable() {
        clearLoadedModels();
        super.onDisable();
    }

    public void clearLoadedModels() {
        for (SwordVariant variant : SWORD_VARIANTS) {
            variant.model.clear();
        }
    }

    public static String[] getVariantLabels() {
        String[] labels = new String[SWORD_VARIANTS.length];
        for (int i = 0; i < SWORD_VARIANTS.length; i++) {
            labels[i] = SWORD_VARIANTS[i].label;
        }
        return labels;
    }

    private static Map<String, SwordVariant> createVariantMap() {
        Map<String, SwordVariant> variants = new HashMap<>(SWORD_VARIANTS.length * 2);
        for (SwordVariant variant : SWORD_VARIANTS) {
            variants.put(variant.label.toLowerCase(Locale.ROOT), variant);
        }
        return variants;
    }

    private static SwordVariant[] createSwordVariants() {
        String[][] data = new String[][]{
                {"Abominable Blade", "abominableblade"},
                {"Abominable Greatsaber", "abominablegreatsaber"},
                {"Abominable Scythe", "abominablescythe"},
                {"Acid Demon", "aciddemon"},
                {"Amethyst Shuriken", "amethyst_shuriken"},
                {"Ancient Royal Greatsword", "ancient_royal_great_sword"},
                {"Aquantic Sacred Blade", "aquantic_sacred_blade"},
                {"Aquantic Trident", "aquantictrident"},
                {"Arcanethyst", "arcanethyst"},
                {"Ashura Blade", "ashura_blade"},
                {"Awakened Lichblade", "awakened_lichblade"},
                {"Blood Edge", "bloodedge"},
                {"Bloody Death", "bloodydeath"},
                {"Bramblethorn", "bramblethorn"},
                {"Brimstone Claymore", "brimstone_claymore"},
                {"Carian Sword", "cariansword"},
                {"Chrono Blade", "chrono_blade"},
                {"Corrupted Mythic Blade", "corruptedmythicblade"},
                {"Creation Splitter", "creationsplitter"},
                {"Crescent Rose", "crescentrose"},
                {"Cyber Katana", "cyberkatana"},
                {"Cyber Mantis Blade", "cybermantisblade"},
                {"Cybernetic Katana", "cybernetickatana"},
                {"Cybernetic Knife", "cyberneticknife"},
                {"Cybernetic Sawblade", "cyberneticsawblade"},
                {"Cyber Sword", "cybersword"},
                {"Dainsleif", "dainsleif"},
                {"Dark Blade", "dark_blade"},
                {"Dark Cleaver", "dark_cleaver"},
                {"Death Knight Dagger", "death_knight_dagger"},
                {"Death Knight Sword", "death_knight_sword"},
                {"Demigod's Unholy Blade", "demigodsunholyblade"},
                {"Demigod's Unholy Halberd", "demigodsunholyhalberd"},
                {"Demonic Blade", "demonicblade"},
                {"Demonic Cleaver", "demoniccleaver"},
                {"Demon Lord's Great Axe", "demonlordsgreataxe"},
                {"Demon Lord's Sword", "demonlordsword"},
                {"Divine Justice", "divine_justice"},
                {"Divine Reaper", "divine_reaper"},
                {"Divine Axe Rhitta", "divineaxerhitta"},
                {"Divine Punisher", "divinepunisher"},
                {"Dragon Slaying Blade", "dragonslayingblade"},
                {"Edge of the Astral Plane", "edgeoftheastralplane"},
                {"Emberblade", "emberblade"},
                {"Enigma", "enigma"},
                {"Epic Sword", "epicsword"},
                {"Estoc", "estoc"},
                {"Excalibur", "excalibur"},
                {"Fallen God Spear", "fallengodspear"},
                {"Fallen God Sword", "fallengodsword"},
                {"Floral Longsword", "floral_longsword"},
                {"Floral Sabre", "floral_sabre"},
                {"Forest Guardian Glaive", "forest_guardian_glaive"},
                {"Frost Axe", "frostaxe"},
                {"Frostblade", "frostblade"},
                {"Frost Scythe", "frostscythe"},
                {"Green Scythe", "greenscythe"},
                {"Hearthflame", "hearthflame"},
                {"Hero Sword", "herosword"},
                {"Holy Moonlight Sword", "holymoonlightsword"},
                {"Hornet's Needle", "hornetsneedle"},
                {"Ice Whisper", "icewhisper"},
                {"Jade Halberd", "jadehalberd"},
                {"Katana", "katana"},
                {"Legendary Sword", "legendarysword"},
                {"Longsword", "longsword"},
                {"Magi Scythe", "magiscythe"},
                {"Masamune", "masamune"},
                {"Mjolnir", "mjolnir"},
                {"Molten Blade", "moltenblade"},
                {"Molten Sword", "moltensword"},
                {"Muramasa", "muramasa"},
                {"Mystical Spellblade", "mysticalspellblade"},
                {"Mythic Blade", "mythicblade"},
                {"Partisan", "partisan"},
                {"Pharaoh's Treasure", "pharaohs_treasure"},
                {"Phoenix Grace", "pheonixgrace"},
                {"Powerfuse Hammer", "powerfusehammer"},
                {"Powerfuse Sword", "powerfusesword"},
                {"Requiem of Hell", "requiem_of_hell"},
                {"Ribbon Cleaver", "ribboncleaver"},
                {"Righteous Relic", "righteous_relic"},
                {"Rivers of Blood", "riversofblood"},
                {"Royal Chakram", "royalchakram"},
                {"Royal Rapier", "royalrapier"},
                {"Sabre", "sabre"},
                {"Scissor Blade", "scissorblade"},
                {"Sculk Cleaver", "sculkcleaver"},
                {"Sculk Scythe", "sculkscythe"},
                {"Sculk Sword", "sculksword"},
                {"Sentinel's Will", "sentinels_will"},
                {"Silverine Blade", "silverine_blade"},
                {"Soul Claws", "soulclaws"},
                {"Soul Edge", "souledge"},
                {"Soul Harvester", "soulharvester"},
                {"Soul Render", "soulrender"},
                {"Soul Stealer", "soulstealer"},
                {"Soul Collector", "soul_collector"},
                {"Soul Devourer", "soul_devourer"},
                {"Star's Edge", "stars_edge"},
                {"Steel Sword", "steelsword"},
                {"Stop Sign", "stop_sign"},
                {"Stormbringer", "stormbringer"},
                {"Storm's Edge", "storms_edge"},
                {"Sunbreak", "sunbreak"},
                {"Tengen's Blade", "tengensblade"},
                {"Terrablade", "terrablade"},
                {"Thousand Demon Daggers", "thousanddemondaggers"},
                {"Thunderbrand", "thunderbrand"},
                {"Thunderbringer", "thunderbringer"},
                {"Toxic Longsword", "toxic_longsword"},
                {"Vampiric Needle", "vampiricneedle"},
                {"Wakizashi", "wakizashi"},
                {"Watcher Claymore", "watcher_claymore"},
                {"Watching Warglaive", "watching_warglaive"},
                {"Waxweaver", "waxweaver"},
                {"Whisperwind", "whisperwind"},
                {"Wickpiercer", "wickpiercer"},
                {"Yoru", "yoru"}
        };

        SwordVariant[] variants = new SwordVariant[data.length];
        for (int i = 0; i < data.length; i++) {
            variants[i] = new SwordVariant(data[i][0], data[i][1]);
        }
        return variants;
    }

    public static final class SwordVariant {
        public final String label;
        public final String modelName;
        private final RuntimeSwordModel model;

        private SwordVariant(String label, String modelName) {
            this.label = label;
            this.modelName = modelName;
            this.model = new RuntimeSwordModel(modelName);
        }
    }

    private static final class RuntimeSwordModel {
        private static final String MODEL_ROOT = "SkyCore/item_replacer/sword/models/";
        private static final String TEXTURE_ROOT = "SkyCore/item_replacer/sword/textures/";
        private static final float PIXEL = 1.0f / 16.0f;
        private static final float MODEL_CENTER = 0.5f;
        private static final int VERTEX_STRIDE = 8;

        private final Minecraft mc = Minecraft.getInstance();
        private final String modelPath;
        private final ArrayList<ModelCube> cubes = new ArrayList<>();
        private final DisplayTransform[] displayTransforms = new DisplayTransform[8];

        private ResourceLocation textureLocation;
        private RenderType renderType;
        private float[] bakedMesh = new float[0];
        private int vertexCount;
        private boolean loaded;

        private RuntimeSwordModel(String modelName) {
            this.modelPath = MODEL_ROOT + modelName + ".json";
        }

        private void clear() {
            loaded = false;
            textureLocation = null;
            renderType = null;
            bakedMesh = new float[0];
            vertexCount = 0;
            cubes.clear();
            for (int i = 0; i < displayTransforms.length; i++) {
                displayTransforms[i] = null;
            }
        }

        private boolean render(
                ItemStack stack,
                ItemCameraTransforms.TransformType transformType,
                MatrixStack matrixStack,
                IRenderTypeBuffer buffer,
                int combinedLight,
                int combinedOverlay
        ) {
            loadIfNeeded();
            if (vertexCount == 0 || renderType == null) {
                return false;
            }

            IVertexBuilder builder = ItemRenderer.getEntityGlintVertexBuilder(
                    buffer,
                    renderType,
                    true,
                    stack.hasEffect()
            );

            matrixStack.push();
            applyDisplayTransform(matrixStack, transformType);
            MatrixStack.Entry entry = matrixStack.getLast();
            drawMesh(builder, entry.getMatrix(), entry.getNormal(), combinedLight, combinedOverlay);
            matrixStack.pop();
            return true;
        }

        private void loadIfNeeded() {
            if (loaded) {
                return;
            }

            loaded = true;
            try (InputStream input = mc.getResourceManager()
                    .getResource(new ResourceLocation("minecraft", modelPath))
                    .getInputStream();
                 Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                readModel(root);
            } catch (Exception exception) {
                System.err.println("[ItemReplacer] Failed to load model " + modelPath + ": "
                        + exception.getMessage());
            }
        }

        private void readModel(JsonObject root) {
            JsonObject texturesObject = root.getAsJsonObject("textures");
            if (texturesObject == null) {
                return;
            }

            Map<String, String> textureMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : texturesObject.entrySet()) {
                textureMap.put(entry.getKey(), entry.getValue().getAsString());
            }

            String primaryTextureKey = resolvePrimaryTextureKey(textureMap, root);
            String primaryTexturePath = textureMap.get(primaryTextureKey);
            if (primaryTexturePath == null) {
                return;
            }

            String textureName = primaryTexturePath.contains("/")
                    ? primaryTexturePath.substring(primaryTexturePath.lastIndexOf('/') + 1)
                    : primaryTexturePath;
            textureLocation = new ResourceLocation("minecraft", TEXTURE_ROOT + textureName + ".png");
            renderType = hasPartialAlpha(textureLocation)
                    ? RenderType.getEntityTranslucent(textureLocation)
                    : RenderType.getEntityCutout(textureLocation);

            JsonArray elements = root.getAsJsonArray("elements");
            if (elements == null) {
                return;
            }

            String primaryTextureReference = "#" + primaryTextureKey;
            for (JsonElement element : elements) {
                JsonObject object = element.getAsJsonObject();
                JsonArray from = object.getAsJsonArray("from");
                JsonArray to = object.getAsJsonArray("to");
                if (from == null || to == null) {
                    continue;
                }

                ModelCube cube = new ModelCube(
                        from.get(0).getAsFloat() * PIXEL,
                        from.get(1).getAsFloat() * PIXEL,
                        from.get(2).getAsFloat() * PIXEL,
                        to.get(0).getAsFloat() * PIXEL,
                        to.get(1).getAsFloat() * PIXEL,
                        to.get(2).getAsFloat() * PIXEL
                );

                JsonObject faces = object.getAsJsonObject("faces");
                if (faces == null || !readFaces(cube, faces, primaryTextureReference)) {
                    continue;
                }

                if (object.has("rotation")) {
                    readRotation(cube, object.getAsJsonObject("rotation"));
                }

                cubes.add(cube);
            }

            bakeMesh();
            readDisplayTransforms(root);
        }

        private boolean readFaces(ModelCube cube, JsonObject faces, String primaryTextureReference) {
            boolean hasFace = false;

            if (faces.has("north")) {
                cube.north = readFace(faces.getAsJsonObject("north"), primaryTextureReference);
                hasFace |= cube.north != null;
            }
            if (faces.has("south")) {
                cube.south = readFace(faces.getAsJsonObject("south"), primaryTextureReference);
                hasFace |= cube.south != null;
            }
            if (faces.has("east")) {
                cube.east = readFace(faces.getAsJsonObject("east"), primaryTextureReference);
                hasFace |= cube.east != null;
            }
            if (faces.has("west")) {
                cube.west = readFace(faces.getAsJsonObject("west"), primaryTextureReference);
                hasFace |= cube.west != null;
            }
            if (faces.has("up")) {
                cube.up = readFace(faces.getAsJsonObject("up"), primaryTextureReference);
                hasFace |= cube.up != null;
            }
            if (faces.has("down")) {
                cube.down = readFace(faces.getAsJsonObject("down"), primaryTextureReference);
                hasFace |= cube.down != null;
            }

            return hasFace;
        }

        private FaceUv readFace(JsonObject face, String primaryTextureReference) {
            if (face.has("texture") && !face.get("texture").getAsString().equals(primaryTextureReference)) {
                return null;
            }

            JsonArray uv = face.getAsJsonArray("uv");
            if (uv == null || uv.size() < 4) {
                return null;
            }

            return new FaceUv(
                    uv.get(0).getAsFloat() * PIXEL,
                    uv.get(1).getAsFloat() * PIXEL,
                    uv.get(2).getAsFloat() * PIXEL,
                    uv.get(3).getAsFloat() * PIXEL
            );
        }

        private void readRotation(ModelCube cube, JsonObject rotation) {
            if (rotation == null || !rotation.has("origin") || !rotation.has("axis") || !rotation.has("angle")) {
                return;
            }

            JsonArray origin = rotation.getAsJsonArray("origin");
            cube.rotationOriginX = origin.get(0).getAsFloat() * PIXEL;
            cube.rotationOriginY = origin.get(1).getAsFloat() * PIXEL;
            cube.rotationOriginZ = origin.get(2).getAsFloat() * PIXEL;
            cube.setRotation(rotation.get("axis").getAsString(), rotation.get("angle").getAsFloat());
        }

        private String resolvePrimaryTextureKey(Map<String, String> textureMap, JsonObject root) {
            if (textureMap.containsKey("0")) {
                return "0";
            }

            Map<String, Integer> faceUseCount = new HashMap<>();
            JsonArray elements = root.getAsJsonArray("elements");
            if (elements != null) {
                for (JsonElement element : elements) {
                    JsonObject faces = element.getAsJsonObject().getAsJsonObject("faces");
                    if (faces == null) {
                        continue;
                    }

                    for (Map.Entry<String, JsonElement> faceEntry : faces.entrySet()) {
                        JsonObject face = faceEntry.getValue().getAsJsonObject();
                        if (!face.has("texture")) {
                            continue;
                        }

                        String reference = face.get("texture").getAsString().replace("#", "");
                        faceUseCount.merge(reference, 1, Integer::sum);
                    }
                }
            }

            String bestKey = null;
            int bestCount = -1;
            for (Map.Entry<String, Integer> entry : faceUseCount.entrySet()) {
                String key = entry.getKey();
                int count = entry.getValue();
                if (!textureMap.containsKey(key) || "particle".equals(key) || count <= bestCount) {
                    continue;
                }

                bestKey = key;
                bestCount = count;
            }

            if (bestKey != null) {
                return bestKey;
            }

            for (String key : textureMap.keySet()) {
                if (!"particle".equals(key)) {
                    return key;
                }
            }

            return textureMap.keySet().iterator().next();
        }

        private boolean hasPartialAlpha(ResourceLocation location) {
            try (InputStream input = mc.getResourceManager().getResource(location).getInputStream();
                 NativeImage image = NativeImage.read(input)) {
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int alpha = image.getPixelRGBA(x, y) >>> 24 & 0xFF;
                        if (alpha > 0 && alpha < 255) {
                            return true;
                        }
                    }
                }
                return false;
            } catch (Exception ignored) {
                return true;
            }
        }

        private void bakeMesh() {
            int faceCount = 0;
            for (ModelCube cube : cubes) {
                faceCount += cube.countFaces();
            }

            float[] baked = new float[faceCount * 4 * VERTEX_STRIDE];
            int offset = 0;
            for (ModelCube cube : cubes) {
                offset = bakeCube(baked, offset, cube);
            }

            bakedMesh = baked;
            vertexCount = offset / VERTEX_STRIDE;
            cubes.clear();
            cubes.trimToSize();
        }

        private int bakeCube(float[] out, int offset, ModelCube cube) {
            float x1 = cube.x1;
            float y1 = cube.y1;
            float z1 = cube.z1;
            float x2 = cube.x2;
            float y2 = cube.y2;
            float z2 = cube.z2;
            FaceUv face;

            if (cube.north != null) {
                face = cube.north;
                offset = writeBakedVertex(out, offset, cube, x2, y1, z1, face.u1, face.v2, 0.0f, 0.0f, -1.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y1, z1, face.u2, face.v2, 0.0f, 0.0f, -1.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y2, z1, face.u2, face.v1, 0.0f, 0.0f, -1.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z1, face.u1, face.v1, 0.0f, 0.0f, -1.0f);
            }

            if (cube.south != null) {
                face = cube.south;
                offset = writeBakedVertex(out, offset, cube, x1, y1, z2, face.u1, face.v2, 0.0f, 0.0f, 1.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y1, z2, face.u2, face.v2, 0.0f, 0.0f, 1.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z2, face.u2, face.v1, 0.0f, 0.0f, 1.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y2, z2, face.u1, face.v1, 0.0f, 0.0f, 1.0f);
            }

            if (cube.east != null) {
                face = cube.east;
                offset = writeBakedVertex(out, offset, cube, x2, y1, z2, face.u1, face.v2, 1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y1, z1, face.u2, face.v2, 1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z1, face.u2, face.v1, 1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z2, face.u1, face.v1, 1.0f, 0.0f, 0.0f);
            }

            if (cube.west != null) {
                face = cube.west;
                offset = writeBakedVertex(out, offset, cube, x1, y1, z1, face.u1, face.v2, -1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y1, z2, face.u2, face.v2, -1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y2, z2, face.u2, face.v1, -1.0f, 0.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y2, z1, face.u1, face.v1, -1.0f, 0.0f, 0.0f);
            }

            if (cube.up != null) {
                face = cube.up;
                offset = writeBakedVertex(out, offset, cube, x1, y2, z1, face.u1, face.v1, 0.0f, 1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y2, z2, face.u1, face.v2, 0.0f, 1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z2, face.u2, face.v2, 0.0f, 1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y2, z1, face.u2, face.v1, 0.0f, 1.0f, 0.0f);
            }

            if (cube.down != null) {
                face = cube.down;
                offset = writeBakedVertex(out, offset, cube, x1, y1, z2, face.u1, face.v1, 0.0f, -1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x1, y1, z1, face.u1, face.v2, 0.0f, -1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y1, z1, face.u2, face.v2, 0.0f, -1.0f, 0.0f);
                offset = writeBakedVertex(out, offset, cube, x2, y1, z2, face.u2, face.v1, 0.0f, -1.0f, 0.0f);
            }

            return offset;
        }

        private int writeBakedVertex(
                float[] out,
                int offset,
                ModelCube cube,
                float x,
                float y,
                float z,
                float u,
                float v,
                float normalX,
                float normalY,
                float normalZ
        ) {
            if (cube.rotationAxis != ModelCube.NO_ROTATION) {
                float dx = x - cube.rotationOriginX;
                float dy = y - cube.rotationOriginY;
                float dz = z - cube.rotationOriginZ;
                float sin = cube.rotationSin;
                float cos = cube.rotationCos;

                switch (cube.rotationAxis) {
                    case ModelCube.ROTATE_X: {
                        y = cube.rotationOriginY + dy * cos - dz * sin;
                        z = cube.rotationOriginZ + dy * sin + dz * cos;
                        float rotatedNormalY = normalY * cos - normalZ * sin;
                        float rotatedNormalZ = normalY * sin + normalZ * cos;
                        normalY = rotatedNormalY;
                        normalZ = rotatedNormalZ;
                        break;
                    }
                    case ModelCube.ROTATE_Y: {
                        x = cube.rotationOriginX + dx * cos + dz * sin;
                        z = cube.rotationOriginZ - dx * sin + dz * cos;
                        float rotatedNormalX = normalX * cos + normalZ * sin;
                        float rotatedNormalZ = -normalX * sin + normalZ * cos;
                        normalX = rotatedNormalX;
                        normalZ = rotatedNormalZ;
                        break;
                    }
                    case ModelCube.ROTATE_Z: {
                        x = cube.rotationOriginX + dx * cos - dy * sin;
                        y = cube.rotationOriginY + dx * sin + dy * cos;
                        float rotatedNormalX = normalX * cos - normalY * sin;
                        float rotatedNormalY = normalX * sin + normalY * cos;
                        normalX = rotatedNormalX;
                        normalY = rotatedNormalY;
                        break;
                    }
                    default:
                        break;
                }
            }

            out[offset++] = x - MODEL_CENTER;
            out[offset++] = y - MODEL_CENTER;
            out[offset++] = z - MODEL_CENTER;
            out[offset++] = u;
            out[offset++] = v;
            out[offset++] = normalX;
            out[offset++] = normalY;
            out[offset++] = normalZ;
            return offset;
        }

        private void readDisplayTransforms(JsonObject root) {
            if (!root.has("display")) {
                return;
            }

            JsonObject display = root.getAsJsonObject("display");
            displayTransforms[0] = readDisplayTransform(display, "thirdperson_righthand");
            displayTransforms[1] = readDisplayTransform(display, "thirdperson_lefthand");
            displayTransforms[2] = readDisplayTransform(display, "firstperson_righthand");
            displayTransforms[3] = readDisplayTransform(display, "firstperson_lefthand");
            displayTransforms[4] = readDisplayTransform(display, "ground");
            displayTransforms[5] = readDisplayTransform(display, "gui");
            displayTransforms[6] = readDisplayTransform(display, "head");
            displayTransforms[7] = readDisplayTransform(display, "fixed");
        }

        private DisplayTransform readDisplayTransform(JsonObject display, String key) {
            if (!display.has(key)) {
                return null;
            }

            JsonObject object = display.getAsJsonObject(key);
            float[] rotation = new float[]{0.0f, 0.0f, 0.0f};
            float[] translation = new float[]{0.0f, 0.0f, 0.0f};
            float[] scale = new float[]{1.0f, 1.0f, 1.0f};

            if (object.has("rotation")) {
                readFloatArray(object.getAsJsonArray("rotation"), rotation);
            }
            if (object.has("translation")) {
                readFloatArray(object.getAsJsonArray("translation"), translation);
            }
            if (object.has("scale")) {
                readFloatArray(object.getAsJsonArray("scale"), scale);
            }

            return new DisplayTransform(rotation, translation, scale);
        }

        private void readFloatArray(JsonArray array, float[] target) {
            for (int i = 0; i < target.length && i < array.size(); i++) {
                target[i] = array.get(i).getAsFloat();
            }
        }

        private void applyDisplayTransform(MatrixStack matrixStack, ItemCameraTransforms.TransformType type) {
            DisplayTransform transform = getDisplayTransform(type);
            if (transform == null) {
                return;
            }

            boolean leftHand = type == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND
                    || type == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
            transform.apply(matrixStack, leftHand);
        }

        private DisplayTransform getDisplayTransform(ItemCameraTransforms.TransformType type) {
            switch (type) {
                case THIRD_PERSON_RIGHT_HAND:
                    return displayTransforms[0];
                case THIRD_PERSON_LEFT_HAND:
                    return displayTransforms[1];
                case FIRST_PERSON_RIGHT_HAND:
                    return displayTransforms[2];
                case FIRST_PERSON_LEFT_HAND:
                    return displayTransforms[3];
                case GROUND:
                    return displayTransforms[4];
                case GUI:
                    return displayTransforms[5];
                case HEAD:
                    return displayTransforms[6];
                case FIXED:
                    return displayTransforms[7];
                default:
                    return null;
            }
        }

        private void drawMesh(
                IVertexBuilder builder,
                Matrix4f matrix,
                Matrix3f normal,
                int light,
                int overlay
        ) {
            int limit = vertexCount * VERTEX_STRIDE;
            for (int i = 0; i < limit; i += VERTEX_STRIDE) {
                builder.pos(matrix, bakedMesh[i], bakedMesh[i + 1], bakedMesh[i + 2])
                        .color(255, 255, 255, 255)
                        .tex(bakedMesh[i + 3], bakedMesh[i + 4])
                        .overlay(overlay)
                        .lightmap(light)
                        .normal(normal, bakedMesh[i + 5], bakedMesh[i + 6], bakedMesh[i + 7])
                        .endVertex();
            }
        }
    }

    private static final class DisplayTransform {
        private final double translationX;
        private final double mirroredTranslationX;
        private final double translationY;
        private final double translationZ;
        private final Quaternion rotation;
        private final Quaternion mirroredRotation;
        private final float scaleX;
        private final float scaleY;
        private final float scaleZ;
        private final boolean hasTranslation;
        private final boolean hasRotation;
        private final boolean hasScale;

        private DisplayTransform(float[] rotation, float[] translation, float[] scale) {
            this.translationX = translation[0] / 16.0f;
            this.mirroredTranslationX = -this.translationX;
            this.translationY = translation[1] / 16.0f;
            this.translationZ = translation[2] / 16.0f;
            this.rotation = new Quaternion(rotation[0], rotation[1], rotation[2], true);
            this.mirroredRotation = new Quaternion(rotation[0], -rotation[1], -rotation[2], true);
            this.scaleX = scale[0];
            this.scaleY = scale[1];
            this.scaleZ = scale[2];
            this.hasTranslation = translation[0] != 0.0f || translation[1] != 0.0f || translation[2] != 0.0f;
            this.hasRotation = rotation[0] != 0.0f || rotation[1] != 0.0f || rotation[2] != 0.0f;
            this.hasScale = scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f;
        }

        private void apply(MatrixStack matrixStack, boolean leftHand) {
            if (hasTranslation) {
                matrixStack.translate(leftHand ? mirroredTranslationX : translationX, translationY, translationZ);
            }
            if (hasRotation) {
                matrixStack.rotate(leftHand ? mirroredRotation : rotation);
            }
            if (hasScale) {
                matrixStack.scale(scaleX, scaleY, scaleZ);
            }
        }
    }

    private static final class ModelCube {
        private static final int NO_ROTATION = 0;
        private static final int ROTATE_X = 1;
        private static final int ROTATE_Y = 2;
        private static final int ROTATE_Z = 3;

        private final float x1;
        private final float y1;
        private final float z1;
        private final float x2;
        private final float y2;
        private final float z2;

        private FaceUv north;
        private FaceUv south;
        private FaceUv east;
        private FaceUv west;
        private FaceUv up;
        private FaceUv down;

        private float rotationOriginX;
        private float rotationOriginY;
        private float rotationOriginZ;
        private int rotationAxis = NO_ROTATION;
        private float rotationSin;
        private float rotationCos = 1.0f;

        private ModelCube(float x1, float y1, float z1, float x2, float y2, float z2) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        private int countFaces() {
            int faces = 0;
            if (north != null) {
                faces++;
            }
            if (south != null) {
                faces++;
            }
            if (east != null) {
                faces++;
            }
            if (west != null) {
                faces++;
            }
            if (up != null) {
                faces++;
            }
            if (down != null) {
                faces++;
            }
            return faces;
        }

        private void setRotation(String axis, float angle) {
            switch (axis) {
                case "x":
                    rotationAxis = ROTATE_X;
                    break;
                case "y":
                    rotationAxis = ROTATE_Y;
                    break;
                case "z":
                    rotationAxis = ROTATE_Z;
                    break;
                default:
                    rotationAxis = NO_ROTATION;
                    return;
            }

            float radians = (float) Math.toRadians(angle);
            rotationSin = (float) Math.sin(radians);
            rotationCos = (float) Math.cos(radians);
        }
    }

    private static final class FaceUv {
        private final float u1;
        private final float v1;
        private final float u2;
        private final float v2;

        private FaceUv(float u1, float v1, float u2, float v2) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }
    }
}
