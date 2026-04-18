using Android.App;
using Android.Content.PM;
using Android.OS;
using Android.Views;
using AndroidX.Core.View;

namespace SlidingNumbersPlus
{
    [Activity(Theme = "@style/Maui.MainTheme.NoActionBar", MainLauncher = true)]
    public class MainActivity : MauiAppCompatActivity
    {
        protected override void OnCreate(Bundle? savedInstanceState)
        {
            base.OnCreate(savedInstanceState);

            // 1. Tell Android not to "fit" the window to system bars
            WindowCompat.SetDecorFitsSystemWindows(Window, false);

            // 2. Allow drawing behind the camera notch/cutout
            if (Build.VERSION.SdkInt >= BuildVersionCodes.P)
            {
                Window.Attributes.LayoutInDisplayCutoutMode = LayoutInDisplayCutoutMode.ShortEdges;
            }

            // 3. Hide bars and set swipe behavior
            var controller = WindowCompat.GetInsetsController(Window, Window.DecorView);
            if (controller != null)
            {
                // Hide both status and navigation bars
                controller.Hide(WindowInsetsCompat.Type.SystemBars());
                // Show them temporarily only when user swipes from the edge
                controller.SystemBarsBehavior = WindowInsetsControllerCompat.BehaviorShowTransientBarsBySwipe;
            }
        }
    }
}
