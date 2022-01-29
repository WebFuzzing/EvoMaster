namespace EvoMaster.Instrumentation.Examples.Branches{
    public interface IBranches{
        int Pos(int x, int y);

        int Neg(int x, int y);

        int Eq(int x, int y);
        
        
        int PosDouble(double x, double y);

        int NegDouble(double x, double y);

        int EqDouble(double x, double y);
    }
}