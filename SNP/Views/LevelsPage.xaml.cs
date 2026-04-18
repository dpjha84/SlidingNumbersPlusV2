using SlidingNumbersPlus.Models;

namespace SlidingNumbersPlus.Views
{
    public partial class LevelsPage : ContentPage
    {
        private record Group(string City, int GridSize, string Img, string Hex);
        private static readonly Group[] Groups =
        {
            new("London",   3, "london.png",  "#E94560"),
            new("Paris",    4, "paris.png",   "#0F3460"),
            new("New York", 5, "newyork.png", "#7B2FBE"),
        };

        public LevelsPage() { InitializeComponent(); }

        protected override void OnAppearing()
        {
            base.OnAppearing();
            var state = GameState.Load();
            container.Children.Clear();
            foreach (var g in Groups) container.Add(Build(g, state));
        }

        private View Build(Group g, GameState state)
        {
            int start  = (g.GridSize - 3) * 5 + 1;
            var accent = Color.FromArgb(g.Hex);
            var wrap   = new VerticalStackLayout { Spacing = 8 };

            // Banner
            var bannerInner = new Grid();
            bannerInner.Children.Add(new Image
            {
                Source = ImageSource.FromFile(g.Img), Aspect = Aspect.AspectFill,
                Opacity = 0.45, HorizontalOptions = LayoutOptions.Fill,
                VerticalOptions = LayoutOptions.Fill,
            });
            bannerInner.Children.Add(new Label
            {
                Text = g.City.ToUpper(), FontSize = 28, FontAttributes = FontAttributes.Bold,
                TextColor = Colors.White, HorizontalTextAlignment = TextAlignment.Center,
                VerticalTextAlignment = TextAlignment.Center, CharacterSpacing = 5,
            });
            bannerInner.Children.Add(new Label
            {
                Text = $"{g.GridSize}\u00D7{g.GridSize} grid  \u2022  Levels {start}\u2013{start+4}",
                FontSize = 11, TextColor = Color.FromArgb("#AAAACC"),
                HorizontalTextAlignment = TextAlignment.Center,
                VerticalTextAlignment   = TextAlignment.End,
                Margin = new Thickness(0, 0, 0, 8),
            });
            wrap.Add(new Border
            {
                HeightRequest = 120,
                StrokeShape = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 14 },
                StrokeThickness = 0, BackgroundColor = Color.FromArgb("#16213E"),
                Content = bannerInner,
            });

            // Button row
            var row = new Grid { ColumnSpacing = 6 };
            for (int i = 0; i < 5; i++)
                row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Star });

            for (int i = 0; i < 5; i++)
            {
                int lvl    = start + i;
                bool locked = lvl > state.MaxLevel;
                bool cur    = lvl == state.MaxLevel;
                var  lbl    = new Label
                {
                    Text = locked ? "\uD83D\uDD12" : lvl.ToString(),
                    FontSize = locked ? 18 : 22, FontAttributes = FontAttributes.Bold,
                    HorizontalTextAlignment = TextAlignment.Center,
                    VerticalTextAlignment   = TextAlignment.Center,
                    TextColor = locked ? Color.FromArgb("#444466") : Colors.White,
                };
                var cell = new Border
                {
                    HeightRequest   = 62,
                    StrokeShape     = new Microsoft.Maui.Controls.Shapes.RoundRectangle { CornerRadius = 12 },
                    StrokeThickness = cur ? 2 : 0,
                    Stroke          = cur ? accent : Colors.Transparent,
                    BackgroundColor = locked ? Color.FromArgb("#0D0D1A")
                                    : cur    ? accent.WithAlpha(0.28f)
                                    :          Color.FromArgb("#16213E"),
                    Content = lbl,
                };
                if (!locked)
                {
                    int cap = lvl;
                    var tap = new TapGestureRecognizer();
                    tap.Tapped += async (_, _) =>
                    {
                        await cell.ScaleToAsync(0.9, 60);
                        await cell.ScaleToAsync(1.0, 60);
                        var s = GameState.Load();
                        s.Level = cap; s.MovesLeft = -1; s.Sequence = new(); s.Save();
                        await Navigation.PushAsync(new GamePage(s, newGame: true));
                    };
                    cell.GestureRecognizers.Add(tap);
                }
                Grid.SetColumn(cell, i);
                row.Children.Add(cell);
            }
            wrap.Add(row);
            return wrap;
        }

        private async void OnBackClicked(object sender, EventArgs e) => await Navigation.PopAsync();
    }
}
