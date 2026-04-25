using System;
using System.Collections.Generic;
using System.Text;

namespace SlidingNumbersPlus
{
    public class SentryHelper
    {
        public static void Send(Action action)
        {
#if DEBUG
            return;
#endif
            Task.Run(() =>
            {
                try
                {
                    action();
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex);
                }
            });
        }

        public static void SendException(Exception exception)
        {
#if DEBUG
            return;
#endif
            Task.Run(() =>
            {
                try
                {
                    SentrySdk.CaptureException(exception);
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex);
                }
            });
        }
    }
}
