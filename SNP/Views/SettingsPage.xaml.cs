using SlidingNumbersPlus.Models;

namespace SlidingNumbersPlus.Views
{
    public partial class SettingsPage : ContentPage
    {
        private bool _ready = false;
        public SettingsPage() { InitializeComponent(); }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            _ready = false;
            var s = GameState.Load();
            lblVersion.Text = AppInfo.Current.VersionString;
            //switchSound.IsToggled = s.SoundOn;
            //switchMusic.IsToggled = s.MusicOn;
            _ready = true;
        }

        private void OnSoundToggled(object sender, ToggledEventArgs e)
        {
            if (!_ready) return;
            var s = GameState.Load(); s.SoundOn = e.Value; s.Save();
        }

        private void OnMusicToggled(object sender, ToggledEventArgs e)
        {
            if (!_ready) return;
            var s = GameState.Load(); s.MusicOn = e.Value; s.Save();
        }

        private async void OnResetClicked(object sender, EventArgs e)
        {
            bool ok = await DisplayAlertAsync("Reset Progress",
                "This clears all levels, stars, and saved games. Continue?",
                "Yes, Reset", "Cancel");
            if (ok) { new GameState().Save(); await DisplayAlertAsync("Done", "Progress reset.", "OK"); }
        }

        private async void OnBackClicked(object sender, EventArgs e) => await Navigation.PopAsync();
    }
}
