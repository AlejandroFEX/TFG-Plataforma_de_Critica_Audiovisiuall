import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Navbar from './components/Navbar'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import FeedPage from './pages/FeedPage'
import SearchPage from './pages/SearchPage'
import RankingPage from './pages/RankingPage'
import ProfilePage from './pages/ProfilePage'

function PrivateRoute({ children }) {
  const { user } = useAuth()
  return user ? children : <Navigate to="/login" />
}

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      <Routes>
        <Route path="/" element={<Navigate to="/feed" />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/feed" element={<FeedPage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/profile/:id" element={<ProfilePage />} />
      </Routes>
    </div>
  )
}