import { Routes, Route, Navigate } from 'react-router-dom';
import NavBar from './components/NavBar';
import ProtectedRoute from './components/ProtectedRoute';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import MovieDetail from './pages/MovieDetail';
import SeatSelection from './pages/SeatSelection';
import Checkout from './pages/Checkout';
import BookingDetail from './pages/BookingDetail';
import MyBookings from './pages/MyBookings';
import Profile from './pages/Profile';
import AdminMovies from './pages/admin/AdminMovies';
import AdminTheaters from './pages/admin/AdminTheaters';
import AdminShows from './pages/admin/AdminShows';
import AdminBookings from './pages/admin/AdminBookings';
import AdminUsers from './pages/admin/AdminUsers';

const admin = (el) => <ProtectedRoute adminOnly>{el}</ProtectedRoute>;
const auth = (el) => <ProtectedRoute>{el}</ProtectedRoute>;

export default function App() {
  return (
    <>
      <NavBar />
      <main className="container py-4">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/movies/:id" element={<MovieDetail />} />
          <Route path="/shows/:id/seats" element={<SeatSelection />} />
          <Route path="/checkout/:bookingId" element={auth(<Checkout />)} />
          <Route path="/bookings" element={auth(<MyBookings />)} />
          <Route path="/bookings/:id" element={auth(<BookingDetail />)} />
          <Route path="/profile" element={auth(<Profile />)} />
          <Route path="/admin/movies" element={admin(<AdminMovies />)} />
          <Route path="/admin/theaters" element={admin(<AdminTheaters />)} />
          <Route path="/admin/shows" element={admin(<AdminShows />)} />
          <Route path="/admin/bookings" element={admin(<AdminBookings />)} />
          <Route path="/admin/users" element={admin(<AdminUsers />)} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <footer className="text-center text-muted small py-4">© {new Date().getFullYear()} CineBook · Movie Ticket Booking</footer>
    </>
  );
}
