import { render, screen } from '@testing-library/react';
import App from './App';

test('renders page header', () => {
  render(<App />);
  const linkElement = screen.getByText(/json data viewer/i);
  expect(linkElement).toBeInTheDocument();
});
