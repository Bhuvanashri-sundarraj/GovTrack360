/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{js,jsx}',
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          50: '#eef2f7',
          100: '#d3dce9',
          200: '#a7b9d3',
          300: '#7b96bd',
          400: '#4f73a7',
          500: '#2f5586',
          600: '#1f3f68',
          700: '#152c4a',
          800: '#0e1e33',
          900: '#08131f',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
