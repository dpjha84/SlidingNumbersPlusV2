using SlidingNumbersPlus.Views;

namespace SlidingNumbersPlus
{
    public partial class App : Application
    {
        public App() { InitializeComponent(); }

        protected override Window CreateWindow(IActivationState? activationState)
        {
            return new Window(new NavigationPage(new HomePage())
            {
                BarBackgroundColor = Color.FromArgb("#1A1A2E"),
                BarTextColor       = Colors.White,
            });
        }
    }
}
