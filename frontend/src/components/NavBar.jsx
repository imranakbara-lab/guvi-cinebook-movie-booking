import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function NavBar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-dark sticky-top">
      <div className="container">
        <Link className="navbar-brand fw-bold" to="/"><span className="brand">●</span> CineBook</Link>
        <button className="navbar-toggler" type="button" onClick={() => document.getElementById('nav').classList.toggle('show')}>
          <span className="navbar-toggler-icon" />
        </button>
        <div className="collapse navbar-collapse" id="nav">
          <ul className="navbar-nav me-auto">
            <li className="nav-item"><NavLink className="nav-link" to="/" end>Movies</NavLink></li>
            {user && <li className="nav-item"><NavLink className="nav-link" to="/bookings">My Bookings</NavLink></li>}
            {isAdmin && (
              <li className="nav-item dropdown">
                <span className="nav-link dropdown-toggle" role="button" onClick={(e) => e.currentTarget.nextSibling.classList.toggle('show')}>Admin</span>
                <ul className="dropdown-menu dropdown-menu-dark" onClick={(e) => e.currentTarget.classList.remove('show')}>
                  <li><Link className="dropdown-item" to="/admin/shows">Shows</Link></li>
                  <li><Link className="dropdown-item" to="/admin/movies">Movies</Link></li>
                  <li><Link className="dropdown-item" to="/admin/theaters">Theaters</Link></li>
                  <li><Link className="dropdown-item" to="/admin/bookings">Bookings</Link></li>
                  <li><Link className="dropdown-item" to="/admin/users">Users</Link></li>
                </ul>
              </li>
            )}
          </ul>
          <ul className="navbar-nav">
            {user ? (
              <>
                <li className="nav-item"><NavLink className="nav-link" to="/profile">👤 {user.name}{isAdmin && ' (Admin)'}</NavLink></li>
                <li className="nav-item"><button className="btn btn-outline-light btn-sm mt-1 ms-lg-2" onClick={() => { logout(); navigate('/'); }}>Logout</button></li>
              </>
            ) : (
              <>
                <li className="nav-item"><NavLink className="nav-link" to="/login">Login</NavLink></li>
                <li className="nav-item"><NavLink className="btn btn-brand btn-sm mt-1 ms-lg-2" to="/register">Sign up</NavLink></li>
              </>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
