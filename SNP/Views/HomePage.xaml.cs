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
            await Bounce(sender as Button);
            var s = GameState.Load();
            bool newGame = s.MovesLeft < 0 || s.Sequence.Count == 0;
            await Navigation.PushAsync(new GamePage(s, newGame));
        }

        private async void OnLevelsClicked(object sender, EventArgs e)
        {
            await Bounce(sender as Button);
            await Navigation.PushAsync(new LevelsPage());
        }

        private async void OnSettingsClicked(object sender, EventArgs e)
        {
            await Bounce(sender as Button);
            await Navigation.PushAsync(new SettingsPage());
        }

        private static async Task Bounce(Button? b)
        {
            if (b == null) return;
            await b.ScaleToAsync(0.93, 70);
            await b.ScaleToAsync(1.00, 70);
        }
    }
}
