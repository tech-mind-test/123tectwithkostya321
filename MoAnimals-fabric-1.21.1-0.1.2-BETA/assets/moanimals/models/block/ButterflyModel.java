// Made with Blockbench 5.0.7
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class ButterflyModel<T extends Butterfly> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "butterflymodel"), "main");
	private final ModelPart root;
	private final ModelPart body;
	private final ModelPart main;
	private final ModelPart wing_left;
	private final ModelPart wing_right;
	private final ModelPart sensor;

	public ButterflyModel(ModelPart root) {
		this.root = root.getChild("root");
		this.body = this.root.getChild("body");
		this.main = this.body.getChild("main");
		this.wing_left = this.body.getChild("wing_left");
		this.wing_right = this.body.getChild("wing_right");
		this.sensor = this.body.getChild("sensor");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, -4.0F));

		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition main = body.addOrReplaceChild("main", CubeListBuilder.create().texOffs(4, 10).addBox(-0.5F, -1.0F, 1.5F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition wing_left = body.addOrReplaceChild("wing_left", CubeListBuilder.create().texOffs(-10, 0).mirror().addBox(-9.5F, 0.0F, -5.0F, 10.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.5F, -0.5F, 4.0F));

		PartDefinition wing_right = body.addOrReplaceChild("wing_right", CubeListBuilder.create().texOffs(-10, 0).addBox(-0.5F, 0.0F, -5.0F, 10.0F, 0.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, -0.5F, 4.0F));

		PartDefinition sensor = body.addOrReplaceChild("sensor", CubeListBuilder.create().texOffs(-4, 11).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 0.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -1.0F, 1.5F, -0.1309F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void setupAnim(Butterfly entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}