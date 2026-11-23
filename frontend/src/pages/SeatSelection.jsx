import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { bookingApi, catalogApi, errorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { formatDateTime, formatPrice } from '../utils';

const MAX_SEATS = 10;

export default function SeatSelection() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [map, setMap] = useState(null);
  const [selected, setSelected] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => catalogApi.seats(id).then((m) => {
    setMap(m);
    // drop any seat someone else just took
    setSelected((sel) => sel.filter((s) => !m.takenSeats.includes(s)));
  }).catch((e) => setError(errorMessage(e))), [id]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 10000);      // refresh availability every 10 s
    return () => clearInterval(timer);
  }, [load]);

  if (error && !map) return <div className="alert alert-danger">{error}</div>;
  if (!map) return <div className="text-center py-5">Loading seats…</div>;

  const show = map.show;
  const isPremium = (seat) => map.premiumRows.some((r) => seat.startsWith(r) && /^\d+$/.test(seat.slice(r.length)));
  const priceOf = (seat) => Number(isPremium(seat) ? show.premiumPrice : show.price);
  const total = selected.reduce((sum, s) => sum + priceOf(s), 0);
  const started = new Date(show.startTime) <= new Date();
  const aisle = Math.ceil(map.seatsPerRow / 2);

  const toggle = (seat) => {
    setError('');
    setSelected((sel) => {
      if (sel.includes(seat)) return sel.filter((s) => s !== seat);
      if (sel.length >= MAX_SEATS) { setError(`You can select up to ${MAX_SEATS} seats`); return sel; }
      return [...sel, seat];
    });
  };

  const proceed = async () => {
    if (!user) { navigate('/login', { state: { from: `/shows/${id}/seats` } }); return; }
    setBusy(true);
    setError('');
    try {
      const booking = await bookingApi.hold(Number(id), selected);
      navigate(`/checkout/${booking.id}`);
    } catch (e) {
      setError(errorMessage(e));
      load();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="row g-4">
      <div className="col-lg-8">
        <div className="card border-0 shadow-sm p-3">
          <div className="mb-3">
            <Link to={`/movies/${show.movieId}`} className="small">← {show.movieTitle}</Link>
            <h5 className="mb-0 mt-1">{show.theaterName}</h5>
            <div className="text-muted small">{formatDateTime(show.startTime)} · {show.city}</div>
          </div>
          {started && <div className="alert alert-warning">This show has already started.</div>}
          <div className="seat-grid">
            <div className="text-center small text-muted mb-1">SCREEN THIS WAY</div>
            <div className="screen" />
            <div className="mt-4">
              {map.rows.map((row) => (
                <div className="seat-row" key={row}>
                  <span className="label">{row}</span>
                  {Array.from({ length: map.seatsPerRow }, (_, i) => {
                    const seat = `${row}${i + 1}`;
                    const taken = map.takenSeats.includes(seat);
                    return (
                      <span key={seat} className="d-flex">
                        {i === aisle && <span className="seat-gap" />}
                        <button type="button" title={`${seat} · ${formatPrice(priceOf(seat))}`}
                                className={`seat ${isPremium(seat) ? 'premium' : ''} ${selected.includes(seat) ? 'selected' : ''}`}
                                disabled={taken || started} onClick={() => toggle(seat)}>{i + 1}</button>
                      </span>
                    );
                  })}
                  <span className="label">{row}</span>
                </div>
              ))}
            </div>
          </div>
          <div className="d-flex flex-wrap gap-3 justify-content-center small mt-3">
            <span><button className="seat" disabled={false} tabIndex={-1} /> Regular {formatPrice(show.price)}</span>
            {map.premiumRows.length > 0 && <span><button className="seat premium" tabIndex={-1} /> Premium {formatPrice(show.premiumPrice)}</span>}
            <span><button className="seat selected" tabIndex={-1} /> Selected</span>
            <span><button className="seat" disabled tabIndex={-1} /> Booked</span>
          </div>
        </div>
      </div>

      <div className="col-lg-4">
        <div className="card border-0 shadow-sm p-3 sticky-lg-top" style={{ top: 80 }}>
          <h6>Booking summary</h6>
          <p className="small text-muted mb-2">{show.movieTitle} · {show.certificate}</p>
          {selected.length === 0
            ? <p className="text-muted">Select your seats on the map.</p>
            : (
              <ul className="list-unstyled small mb-2">
                {selected.map((s) => <li key={s} className="d-flex justify-content-between"><span>{s}{isPremium(s) ? ' (Premium)' : ''}</span><span>{formatPrice(priceOf(s))}</span></li>)}
              </ul>
            )}
          <div className="d-flex justify-content-between fw-bold border-top pt-2 mb-3"><span>Total</span><span>{formatPrice(total)}</span></div>
          {error && <div className="alert alert-danger py-2 small">{error}</div>}
          <button className="btn btn-brand w-100" disabled={selected.length === 0 || busy || started} onClick={proceed}>
            {busy ? 'Holding seats…' : user ? `Proceed to pay ${formatPrice(total)}` : 'Login to continue'}
          </button>
          <p className="small text-muted mt-2 mb-0">Seats are held for 10 minutes while you pay. Availability refreshes automatically.</p>
        </div>
      </div>
    </div>
  );
}
