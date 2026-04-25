using SlidingNumbersPlus.Models;

namespace SlidingNumbersPlus.Views
{
    public partial class HomePage : ContentPage
    {
        public HomePage() { InitializeComponent(); }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            var s = GameState.Load();
            lblStars.Text = s.StarCount.ToString();
            imgBg.Source  = ImageSource.FromFile(GameState.CityImage(s.GridSize));
            lblSub.Text   = $"LEVEL {s.MaxLevel}  \u2022  {GameState.CityName(s.GridSize).ToUpper()}";
            btnPlay.Text  = $"\u25B6  PLAY  LEVEL {s.MaxLevel}";
        }

        private async void OnPlayClicked(object sender, EventArgs e)
        {
            var s = GameState.Load();
            bool newGame = s.MovesLeft < 0 || s.Sequence.Count == 0;

            GamePage page;
            try
            {
                page = new GamePage(s, newGame);
                System.Diagnostics.Debug.WriteLine("GamePage constructed OK");
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"GamePage ctor FAILED: {ex}");
                return;
            }

            try
            {
                var navPage = Application.Current!.MainPage as NavigationPage;
                await navPage!.PushAsync(page);
                System.Diagnostics.Debug.WriteLine("Push OK");
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"PushAsync FAILED: {ex}");
            }
        }

        private async void OnLevelsClicked(object sender, EventArgs e)
        {
            var navPage = Application.Current!.MainPage as NavigationPage;
            await navPage!.PushAsync(new LevelsPage());
        }

        private async void OnSettingsClicked(object sender, EventArgs e)
        {
            var navPage = Application.Current!.MainPage as NavigationPage;
            await navPage!.PushAsync(new SettingsPage());
        }

        private static async Task Bounce(Button? b)
        {
            if (b == null) return;
            await b.ScaleToAsync(0.93, 70);
            await b.ScaleToAsync(1.00, 70);
        }
    }
}
