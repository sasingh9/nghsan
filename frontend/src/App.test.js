import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the main app layout', () => {
  render(<App />);
  const titleElement = screen.getByText(/Trade Manager/i);
  expect(titleElement).toBeInTheDocument();
});
