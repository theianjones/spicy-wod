module.exports = {
  content: ["./src/**/*"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["DDSans", "sans-serif"],
        display: ["Mazer"],
      },
      colors: {
        "brand-pink": "#F29ADA",
        "brand-background": "#FFF1F3",
        "brand-teal": "#83F2B3",
        "darker-brand-teal": "rgb(59 201 121)",
        "brand-purple": "#CB8FF2",
        "brand-blue": "#95A2FA",
        "brand-yellow": "#FFF68F",
        black: "#0D0000",
        "brand-red": "#A61420",
        "brand-red-bright": "#E82642",
        "brand-red-dark": "#400101",
      },
    },
  },
  plugins: [require("@tailwindcss/forms")],
};
