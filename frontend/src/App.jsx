import { Routes, Route, Navigate, Link } from 'react-router-dom'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import FeedPage from './pages/FeedPage'
import SearchPage from './pages/SearchPage'
import RankingPage from './pages/RankingPage'
import ProfilePage from './pages/ProfilePage'

function NotFound() {
  return (
    <div className="page py-24 text-center">
      <h1 className="mb-2 text-3xl font-semibold">Página no encontrada</h1>
      <Link to="/feed" className="text-gem hover:underline">Volver al feed</Link>
    </div>
  )
}

export default function App() {
  return (
    <div className="min-h-screen">
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/feed" />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/feed" element={<FeedPage />} />
          <Route path="/search" element={<SearchPage />} />
          <Route path="/ranking" element={<RankingPage />} />
          <Route path="/profile/:id" element={<ProfilePage />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </div>
  )
}
