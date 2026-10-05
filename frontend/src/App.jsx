import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import MarinesPage from './pages/MarinesPage.jsx';
import ChaptersPage from './pages/ChaptersPage.jsx';
import CoordinatesPage from './pages/CoordinatesPage.jsx';
import OperationsPage from './pages/OperationsPage.jsx';
import { useLiveStatus } from './live.js';

const STATUS_TEXT = { online: 'онлайн', offline: 'нет связи', connecting: 'подключение' };

export default function App() {
  const status = useLiveStatus();
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">SpaceMarine</div>
        <nav className="menu">
          <NavLink to="/marines">Десантники</NavLink>
          <NavLink to="/chapters">Ордены</NavLink>
          <NavLink to="/coordinates">Координаты</NavLink>
          <NavLink to="/operations">Спец. Операции</NavLink>
        </nav>
        <div className={'live live-' + status} title="Соединение для обновлений в реальном времени">
          <span className="dot" /> {STATUS_TEXT[status]}
        </div>
      </header>
      <main className="content">
        <Routes>
          <Route path="/marines" element={<MarinesPage />} />
          <Route path="/chapters" element={<ChaptersPage />} />
          <Route path="/coordinates" element={<CoordinatesPage />} />
          <Route path="/operations" element={<OperationsPage />} />
          <Route path="*" element={<Navigate to="/marines" replace />} />
        </Routes>
      </main>
    </div>
  );
}
