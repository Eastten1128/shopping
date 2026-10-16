import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { BrowserRouter } from 'react-router';
import AuthProvider from './auth/AuthProvider.tsx';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('root element를 찾을 수 없음.');
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
)
