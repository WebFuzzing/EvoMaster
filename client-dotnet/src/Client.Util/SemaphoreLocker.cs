using System;
using System.Threading;
using System.Threading.Tasks;

namespace Client.Util {
    public class SemaphoreLocker {
        private readonly SemaphoreSlim _semaphore = new SemaphoreSlim (1, 1);

        public async Task LockAsync (Func<Task> worker) {
            await _semaphore.WaitAsync ();
            try {
                await worker ();
            } finally {
                _semaphore.Release ();
            }
        }

        public async Task<T> LockAsync<T> (Func<Task<T>> worker) {
            await _semaphore.WaitAsync ();

            T result = default;

            try {
                result = await worker ();
            } finally {
                _semaphore.Release ();
            }

            return result;
        }
    }
}