# The SUT is intentionally stateless: no persisted models.
# Endpoints return deterministic, computed responses so that the tests generated
# by the black-box fuzzer produce the same result every time they are re-run.
