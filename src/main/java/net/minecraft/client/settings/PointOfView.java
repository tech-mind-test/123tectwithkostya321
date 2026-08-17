package net.minecraft.client.settings;

public enum PointOfView
{
    FIRST_PERSON(true, false),
    THIRD_PERSON_BACK(false, false),
    THIRD_PERSON_FRONT(false, true);

    private static final PointOfView[] POINT_OF_VIEWS = values();
    private boolean firstPerson;
    private boolean thirdPersonFront;

    private PointOfView(boolean p_i242049_3_, boolean p_i242049_4_)
    {
        this.firstPerson = p_i242049_3_;
        this.thirdPersonFront = p_i242049_4_;
    }

    public boolean firstPerson()
    {
        return this.firstPerson;
    }

    public boolean thirdPersonFront()
    {
        return this.thirdPersonFront;
    }

    public PointOfView func_243194_c()
    {
        return POINT_OF_VIEWS[(this.ordinal() + 1) % POINT_OF_VIEWS.length];
    }
}
