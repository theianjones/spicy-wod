module.exports = {
  content: ["./src/**/*"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["DDSans", "sans-serif"],
        display: ["Mazer"],
      },
      colors: {
        "brand-pink": "#FFD8F4",
        "brand-background": "#FFF1F3",
        "brand-teal": "#83F2B3",
        "brand-purple": "#CB8FF2",
        black: "#0D0000",
        "brand-red": "#A61420",
        "brand-red-bright": "#E82642",
        "brand-red-dark": "#400101",
      },
    },
  },
  plugins: [require("@tailwindcss/forms")],
};
