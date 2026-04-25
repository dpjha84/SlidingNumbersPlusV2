using CommunityToolkit.Maui;
using Microsoft.Extensions.Logging;
using SkiaSharp.Views.Maui.Controls.Hosting;

namespace SlidingNumbersPlus
{
    public static class MauiProgram
    {
        public static MauiApp CreateMauiApp()
        {
            var builder = MauiApp.CreateBuilder();
            builder
                .UseMauiApp<App>()
                .UseMauiCommunityToolkit()
                .UseSkiaSharp()
                .ConfigureFonts(fonts =>
                {
                    fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                    fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
                })
                .UseSentry(options =>
                {
                    // The DSN is the only required setting.
                    options.Dsn = "https://40a8d8b804934ad18423f1115600179d@o4505365364998144.ingest.us.sentry.io/4505443665969152";
#if DEBUG
                    options.Debug = true;
                    options.TracesSampleRate = 1.0;
#else
                    options.Debug = false;
                    options.TracesSampleRate = 0.1;
#endif
                });
#if DEBUG
            builder.Logging.AddDebug();
#endif
            return builder.Build();
        }
    }
}