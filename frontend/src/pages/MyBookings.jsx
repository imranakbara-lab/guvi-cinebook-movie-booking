import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { bookingApi, errorMessage } from '../api/client';
import { formatDateTime, formatPrice, statusBadge } from '../utils';

export default function MyBookings() {
  const [items, setItems] = useState([]);
  const [tab, setTab] = useState('upcoming');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { bookingApi.my().then(setItems).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false)); }, []);

  const now = new Date();
  const shown = items.filter((b) => (tab === 'upcoming'
    ? new Date(b.show.startTime) > now && ['CONFIRMED', 'PENDING_PAYMENT'].includes(b.status)
    : !(new Date(b.show.startTime) > now && ['CONFIRMED', 'PENDING_PAYMENT'].includes(b.status))));

  return (
    <>
      <h3 className="mb-3">My Bookings</h3>
      <ul className="nav nav-pills mb-3">
        {[['upcoming', 'Upcoming'], ['past', 'Past & cancelled']].map(([k, l]) => (
          <li className="nav-item" key={k}><button className={`nav-link ${tab === k ? 'active bg-dark' : 'text-dark'}`} onClick={() => setTab(k)}>{l}</button></li>
        ))}
      </ul>
      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <p>Loading…</p>}
      {!loading && shown.length === 0 && <div className="text-center text-muted py-5">No bookings here. <Link to="/">Browse movies</Link></div>}
      <div className="row g-3">
        {shown.map((b) => (
          <div className="col-md-6" key={b.id}>
            <Link to={`/bookings/${b.id}`} className="text-decoration-none text-dark">
              <div className="card border-0 shadow-sm h-100 movie-card">
                <div className="card-body d-flex gap-3">
                  {b.show.posterUrl && <img src={b.show.posterUrl} alt="" style={{ width: 70, height: 100, objectFit: 'cover' }} className="rounded" />}
                  <div className="flex-grow-1">
                    <div className="d-flex justify-content-between">
                      <h6 className="mb-1">{b.show.movieTitle}</h6>
                      <span className={`badge bg-${statusBadge(b.status)} align-self-start`}>{b.status.replace('_', ' ')}</span>
                    </div>
                    <div className="small text-muted">{b.show.theaterName}, {b.show.city}</div>
                    <div className="small">{formatDateTime(b.show.startTime)}</div>
                    <div className="small mt-1">Seats <strong>{b.seats.join(', ')}</strong> · {formatPrice(b.amount)} · <span className="font-monospace">{b.reference}</span></div>
                  </div>
                </div>
              </div>
            </Link>
          </div>
        ))}
      </div>
    </>
  );
}
