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
    }
}