using Microsoft.Maui.Controls.Shapes;
using SkiaSharp.Extended.UI.Controls;
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
            System.Diagnostics.Debug.WriteLine($"GamePage ctor. Level={state?.Level} GridSize={state?.GridSize}");
            InitializeComponent();
            System.Diagnostics.Debug.WriteLine("InitializeComponent done");
            _state = state;
            _newGame = newGame;
            _vm = new GameViewModel();
            BindingContext = _vm;
            System.Diagnostics.Debug.WriteLine("VM created");
            _vm.TilesUpdated += OnTilesUpdated;
            _vm.GameReady += OnGameReady;
            //_vm.WinTriggered += async (stars) => await ShowWin(stars);
            //_vm.FailTriggered += ShowFail;
            System.Diagnostics.Debug.WriteLine("GamePage ctor done");
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            imgBg.Source = ImageSource.FromFile(GameState.CityImage(_state.GridSize));
            BuildGrid(_state.GridSize);
            UpdateHud();
            _vm.StartGame(_state, _newGame);
        }

        //protected override void OnNavigatedTo(NavigatedToEventArgs args)
        //{
        //    base.OnNavigatedTo(args);
        //    // Page is fully on screen and laid out — safe to start game now
        //    _vm.StartGame(_state, _newGame);
        //}

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

            // 1. Properly initialize the reference arrays
            _cells = new Border[size, size];
            _labels = new Label[size, size];

            double screenWidth = DeviceDisplay.MainDisplayInfo.Width / DeviceDisplay.MainDisplayInfo.Density;
            double gridMargin = 40;
            double availableWidth = screenWidth - gridMargin;
            double cell = Math.Floor(availableWidth / size);

            for (int i = 0; i < size; i++)
            {
                puzzleGrid.RowDefinitions.Add(new RowDefinition { Height = cell });
                puzzleGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = cell });
            }

            for (int r = 0; r < size; r++)
            {
                for (int c = 0; c < size; c++)
                {
                    var lbl = new Label
                    {
                        FontSize = cell * 0.36,
                        FontAttributes = FontAttributes.Bold,
                        HorizontalTextAlignment = TextAlignment.Center,
                        VerticalTextAlignment = TextAlignment.Center,
                        TextColor = Colors.White,
                    };

                    var border = new Border
                    {
                        StrokeShape = new RoundRectangle { CornerRadius = 8 },
                        StrokeThickness = 0,
                        Margin = 2,
                        Content = lbl,
                        BackgroundColor = Color.FromArgb("#2A2A4E")
                    };

                    int row = r, col = c;

                    var tap = new TapGestureRecognizer();
                    tap.Tapped += (_, _) =>
                    {
                        _vm.Tap(row, col);
                        UpdateHud();
                    };
                    border.GestureRecognizers.Add(tap);

                    // 2. CRITICAL FIX: Store the references so RenderTile can find them later
                    _cells[r, c] = border;
                    _labels[r, c] = lbl;

                    // 3. Add to Grid with proper positioning
                    puzzleGrid.Children.Add(border);
                    Grid.SetRow(border, r);
                    Grid.SetColumn(border, c);
                }
            }

            _built = true;
        }

        private void OnGameReady()
        {
            // 4. Ensure UI logic runs on the Main Thread
            MainThread.BeginInvokeOnMainThread(async () =>
            {
                if (!_built)
                {
                    BuildGrid(_vm.GridSize);
                }
                // Inside GamePage constructor
                _vm.WinTriggered += async (stars) => await ShowWin(stars);
                _vm.FailTriggered += ShowFail;
                // 5. Force initial render of all tiles once grid is ready
                foreach (var tile in _vm.AllTiles())
                {
                    RenderTile(tile);
                }

                loadingOverlay.IsVisible = false;
                puzzleGrid.IsVisible = true;
                UpdateHud();
                puzzleGrid.SizeChanged += OnPuzzleGridReady;
            });
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
            if (t.Row >= _cells.GetLength(0) || t.Col >= _cells.GetLength(1)) return;

            var border = _cells[t.Row, t.Col];
            var lbl = _labels[t.Row, t.Col];
            if (border == null || lbl == null) return;

            border.Opacity = 1;
            if (t.IsEmpty)
            {
                border.BackgroundColor = Colors.Transparent;
                border.Stroke = Colors.Transparent;
                border.StrokeThickness = 0;
                lbl.Text = string.Empty;
            }
            else
            {
                border.BackgroundColor = t.IsCorrect ? Colors.Green : Colors.Red;
                border.Stroke = Colors.White;
                border.StrokeThickness = t.IsCorrect ? 2 : 1;
                lbl.Text = t.Label;
                lbl.TextColor = Colors.White;
            }
        }

        private async void OnPuzzleGridReady(object? sender, EventArgs e)
        {
            puzzleGrid.SizeChanged -= OnPuzzleGridReady;
            await Task.Delay(50);

            System.Diagnostics.Debug.WriteLine($"Before anim: Opacity={puzzleGrid.Opacity} TranslationX={puzzleGrid.TranslationX} IsVisible={puzzleGrid.IsVisible}");

            await Task.WhenAll(
                puzzleGrid.TranslateToAsync(0, 0, 500, Easing.CubicOut),
                puzzleGrid.FadeToAsync(1, 400, Easing.CubicOut)
            );
            await puzzleGrid.ScaleToAsync(1.04, 150, Easing.CubicOut);
            await puzzleGrid.ScaleToAsync(1.00, 150, Easing.CubicIn);
            if (_cells.GetLength(0) > 0)
            {
                var firstCell = _cells[0, 0];
                System.Diagnostics.Debug.WriteLine($"cell[0,0] W={firstCell.Width} H={firstCell.Height} Opacity={firstCell.Opacity} BgColor={firstCell.BackgroundColor}");
            }

            puzzleGrid.InputTransparent = false;
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

        private async Task ShowWin(int stars)
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                victoryAnim.IsVisible = true;

                PlayAnimation(trophyAnimation);
                PlayAnimation(confettiAnimation);

                //winOverlay.IsVisible = true; // Start playback

                if (stars == 1)
                    PlayAnimation(starsAnimation1);

                else if (stars == 2)
                {
                    await Task.Delay(500);
                    PlayAnimation(starsAnimation1);
                    await Task.Delay(500);
                    PlayAnimation(starsAnimation2);
                }

                else
                {
                    await Task.Delay(500);
                    PlayAnimation(starsAnimation1);
                    await Task.Delay(500);
                    PlayAnimation(starsAnimation2);
                    await Task.Delay(500);
                    PlayAnimation(starsAnimation3);
                }
            });
        }

        private void PlayAnimation(SKLottieView view)
        {
            //await Task.Delay(1000);
            view.IsEnabled = true;
            view.IsVisible = true;
            view.IsAnimationEnabled = true;
        }

        private async void ShowFail()
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                failureAnim.IsVisible = true;
                failureLottieAnim.IsVisible = true;
                failureLottieAnim.IsAnimationEnabled = true;
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
            failureAnim.IsVisible    = false;
            loadingOverlay.IsVisible = true;
            _built                   = false;
            _vm.StartGame(_state, newGame: true);
        }

        private void OnBuyMovesClicked(object sender, EventArgs e)
        {
            _vm.BuyMoves();
            failureAnim.IsVisible = false;
            UpdateHud();
        }

        private static async Task TapAnim(Border b)
        {
            await b.ScaleToAsync(0.88, 55);
            await b.ScaleToAsync(1.00, 55);
        }
    }
}
