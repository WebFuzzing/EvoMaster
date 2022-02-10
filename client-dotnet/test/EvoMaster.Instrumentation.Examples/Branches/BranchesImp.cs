namespace EvoMaster.Instrumentation.Examples.Branches{
    public class BranchesImp : IBranches{
        public int Pos(int x, int y){
            if(x>0){
                return 0;
            }

            if(y >= 0){
                return 1;
            }

            return 2;
        }

        public int Neg(int x, int y){
            if(x<0){
                return 3;
            }

            if(y <= 0){
                return 4;
            }

            return 5;
        }

        public int Eq(int x, int y){
            if(x==0) {
                return 6;
            }
            
            if(y!=0) {
                return 7;
            }

            return 8;
        }
        
        public int PosDouble(double x, double y){
            if(x>0){
                return 9;
            }

            if(y >= 0){
                return 10;
            }

            return 11;
        }

        public int NegDouble(double x, double y){
            if(x<0){
                return 12;
            }

            if(y <= 0){
                return 13;
            }

            return 14;
        }

        public int EqDouble(double x, double y){
            if(x==0) {
                return 15;
            }
            
            if(y!=0) {
                return 16;
            }

            return 17;
        }
    }
}