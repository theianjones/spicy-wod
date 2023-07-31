module.exports = {
  content: ["./src/**/*"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["DDSans", "sans-serif"],
        display: ["Mazer"],
      },
      colors: {
        "brand-pink": "#FD8FF4",
        "brand-background": "#FFF1F3",
        "brand-teal": "#73E59E",
        "darker-brand-teal": "rgb(59 201 121)",
        "brand-purple": "#95A2FA",
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
