using SlidingNumbersPlus.Models;
using SlidingNumbersPlus.Services;
using SlidingNumbersPlus.ViewModels;

namespace SlidingNumbersPlus.Views
{
    public partial class GamePage : ContentPage
    {
        private readonly GameViewModel _vm      = new();
        private readonly GameState     _state;
        private readonly bool          _newGame;
        private Border[,]  _cells   = new Border[0, 0];
        private Label[,]   _labels  = new Label[0, 0];
        private bool       _built   = false;

        public GamePage(GameState state, bool newGame)
        {
            InitializeComponent();
            _state   = state;
            _newGame = newGame;
            _vm.TilesUpdated += OnTilesUpdated;
            _vm.GameReady    += OnGameReady;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            imgBg.Source = ImageSource.FromFile(GameState.CityImage(_state.GridSize));
            UpdateHud();
            _vm.StartGame(_state, _newGame);
        }

        protected override void OnDisappearing()
        {
            base.OnDisappearing();
            _vm.StopTimer();
        }

        protected override bool OnBackButtonPressed()
        {
            _vm.StopTimer();
            Navigation.PopToRootAsync();
            return true;
        }

        // ── Grid builder ──────────────────────────────────────────────────
        private void BuildGrid(int size)
        {
            puzzleGrid.Children.Clear();
            puzzleGrid.RowDefinitions.Clear();
            puzzleGrid.ColumnDefinitions.Clear();
            _cells  = new Border[size, size];
            _labels = new Label[size, size];
            _built  = true;

            double screen = Math.Min(
                DeviceDisplay.MainDisplayInfo.Width  / DeviceDisplay.MainDisplayInfo.Density,
                DeviceDisplay.MainDisplayInfo.Height / DeviceDisplay.MainDisplayInfo.Density * 0.58);
            double cell = (screen - 32) / size;

            for (int i = 0; i < size; i++)
            {
                puzzleGrid.RowDefinitions.Add(new RowDefinition    { Height = cell });
                puzzleGrid.ColumnDefinitions.Add(new ColumnDefinition { Width  = cell });
            }

            for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
            {
                var lbl = new Label
                {
                    FontSize = cell * 0.36, FontAttributes = FontAttributes.Bold,
                    HorizontalTextAlignment = TextAlignment.Center,
                    VerticalTextAlignment   = TextAlignment.Center,
                    TextColor = Colors.White,
                };
                var border = new Border
                {
                    StrokeShape     = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 10 },
                    StrokeThickness = 0,
                    Margin          = 3,
                    Content         = lbl,
                };

                int row = r, col = c;
                var tap = new TapGestureRecognizer();
                tap.Tapped += (_, _) =>
                {
                    _vm.Tap(row, col);
                    UpdateHud();
                    _ = TapAnim(border);
                };
                border.GestureRecognizers.Add(tap);
                _cells[r, c]  = border;
                _labels[r, c] = lbl;
                Grid.SetRow(border, r);
                Grid.SetColumn(border, c);
                puzzleGrid.Children.Add(border);
            }
        }

        // ── Tile rendering ────────────────────────────────────────────────
        private void OnTilesUpdated(List<Tile> tiles)
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                foreach (var t in tiles) RenderTile(t);
            });
        }

        private void RenderTile(Tile t)
        {
            if (!_built) return;
            var border = _cells[t.Row, t.Col];
            var lbl    = _labels[t.Row, t.Col];
            border.Opacity = 0.5;// + 0.5 * (t.IsEmpty ? 0 : 1);
            if (t.IsEmpty)
            {
                border.BackgroundColor = Colors.Transparent;
                border.Stroke          = Colors.Transparent;
                border.StrokeThickness = 0;
                lbl.Text               = string.Empty;
            }
            else
            {
                border.BackgroundColor = t.IsCorrect
                    ? Colors.Green : Colors.Red;
                border.Stroke = Colors.White;
                border.StrokeThickness = t.IsCorrect ? 2 : 1;
                lbl.Text      = t.Label;
                lbl.TextColor = Colors.White;
            }
        }

        private void OnGameReady()
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                if (!_built) BuildGrid(_vm.GridSize);
                foreach (var t in _vm.AllTiles()) RenderTile(t);
                loadingOverlay.IsVisible = false;
                _vm.WinTriggered += ShowWin;
                _vm.FailTriggered += ShowFail;
                UpdateHud();
            });
        }

        // ── HUD & overlays ────────────────────────────────────────────────
        private void UpdateHud()
        {
            lblLevel.Text = $"LEVEL {_vm.Level}";
            lblCity.Text = _vm.CityName.ToUpper();
            lblMoves.Text = _vm.MovesLeft.ToString();
            lblMoves.TextColor = Color.FromArgb(_vm.MovesColor);
            lblStars.Text = _vm.StarCount.ToString();
        }

        private async void ShowWin(int stars)
        {
            await MainThread.InvokeOnMainThreadAsync(async () => {
                //victoryAnim.IsAnimationEnabled = true; // Start playback
                //winOverlay.IsVisible = true;
                lblStarsEarned.Text = string.Join(" ", Enumerable.Repeat("⭐", stars));

                // Reset state for animation
                winOverlay.Opacity = 0;
                winOverlay.Scale = 0.8;
                winOverlay.IsVisible = true;

                // Animate in: Fade and Scale up simultaneously
                await Task.WhenAll(
                    winOverlay.FadeToAsync(1, 400, Easing.CubicOut),
                    winOverlay.ScaleToAsync(1, 400, Easing.BounceOut)
                );
            });
        }

        private async void ShowFail()
        {
            await MainThread.InvokeOnMainThreadAsync(async () => {
                btnBuyMoves.IsEnabled = _vm.CanBuyMoves();

                failOverlay.Opacity = 0;
                failOverlay.TranslationY = 50; // Start slightly lower
                failOverlay.IsVisible = true;

                // Animate in: Fade and Slide up
                await Task.WhenAll(
                    failOverlay.FadeToAsync(1, 400, Easing.CubicOut),
                    failOverlay.TranslateToAsync(0, 0, 400, Easing.CubicOut)
                );
            });
        }

        // ── Handlers ─────────────────────────────────────────────────────
        private async void OnBackClicked(object sender, EventArgs e)
        {
            _vm.StopTimer();
            await Navigation.PopToRootAsync();
        }

        private async void OnNextLevelClicked(object sender, EventArgs e)
        {
            var s = GameState.Load();
            await Navigation.PushAsync(new GamePage(s, newGame: true));
        }

        private void OnRetryClicked(object sender, EventArgs e)
        {
            failOverlay.IsVisible    = false;
            loadingOverlay.IsVisible = true;
            _built                   = false;
            _vm.StartGame(_state, newGame: true);
        }

        private void OnBuyMovesClicked(object sender, EventArgs e)
        {
            _vm.BuyMoves();
            failOverlay.IsVisible = false;
            UpdateHud();
        }

        private static async Task TapAnim(Border b)
        {
            await b.ScaleToAsync(0.88, 55);
            await b.ScaleToAsync(1.00, 55);
        }
    }
}
