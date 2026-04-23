namespace EvoMaster.Instrumentation {
    public static class Program {
        public static void Main(string[] args) {
            var instrumentator = new Instrumentator();

            instrumentator.Instrument(args[0], args[1]);
        }
    }
}