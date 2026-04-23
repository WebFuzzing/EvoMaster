namespace EvoMaster.Controller {
    public class InstrumentedSutStarter {
        private readonly SutController _sutController;

        public InstrumentedSutStarter(SutController sutController) {
            _sutController = sutController;
        }

        public bool Start() => _sutController.StartTheControllerServer();
    }
}