import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { catalogApi, errorMessage } from '../api/client';
import DateStrip from '../components/DateStrip';
import { formatDate, formatPrice, formatTime, isoDate, POSTER_FALLBACK } from '../utils';

export default function MovieDetail() {
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const date = params.get('date') || isoDate(new Date());
  const [movie, setMovie] = useState(null);
  const [shows, setShows] = useState([]);
  const [city, setCity] = useState('');
  const [loadingShows, setLoadingShows] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { catalogApi.movie(id).then(setMovie).catch((e) => setError(errorMessage(e))); }, [id]);
  useEffect(() => {
    setLoadingShows(true);
    catalogApi.showsForMovie(id, { date }).then(setShows).catch((e) => setError(errorMessage(e))).finally(() => setLoadingShows(false));
  }, [id, date]);

  const cities = [...new Set(shows.map((s) => s.city))].sort();
  const byTheater = useMemo(() => {
    const map = new Map();
    shows.filter((s) => !city || s.city === city).forEach((s) => {
      if (!map.has(s.theaterId)) map.set(s.theaterId, { name: s.theaterName, city: s.city, address: s.address, shows: [] });
      map.get(s.theaterId).shows.push(s);
    });
    return [...map.values()];
  }, [shows, city]);

  if (error && !movie) return <div className="alert alert-danger">{error} <Link to="/">Back</Link></div>;
  if (!movie) return <div className="text-center py-5">Loading…</div>;

  return (
    <>
      <div className="row g-4 mb-4">
        <div className="col-md-3 col-5">
          <img src={movie.posterUrl || POSTER_FALLBACK} onError={(e) => { e.currentTarget.src = POSTER_FALLBACK; }} className="poster rounded-3 shadow" alt={movie.title} />
        </div>
        <div className="col-md-9 col-7">
          <h2 className="fw-bold">{movie.title}</h2>
          <p className="mb-2">
            {movie.certificate && <span className="badge bg-secondary me-2">{movie.certificate}</span>}
            <span className="text-muted">{movie.genre} · {movie.language} · {Math.floor(movie.durationMinutes / 60)}h {movie.durationMinutes % 60}m</span>
          </p>
          {movie.rating != null && <p className="fs-5 mb-2">⭐ {movie.rating}/10</p>}
          {movie.releaseDate && <p className="small text-muted">Released {formatDate(movie.releaseDate)}</p>}
          <p className="d-none d-md-block">{movie.description}</p>
        </div>
        <p className="d-md-none">{movie.description}</p>
      </div>

      <div className="card border-0 shadow-sm p-3 mb-3">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2">
          <DateStrip value={date} onChange={(v) => setParams({ date: v })} />
          {cities.length > 1 && (
            <select className="form-select w-auto" value={city} onChange={(e) => setCity(e.target.value)}>
              <option value="">All cities</option>
              {cities.map((c) => <option key={c}>{c}</option>)}
            </select>
          )}
        </div>
      </div>

      <h5 className="mb-3">Showtimes on {formatDate(date)}</h5>
      {loadingShows && <p className="text-muted">Loading shows…</p>}
      {!loadingShows && byTheater.length === 0 && <div className="alert alert-light">No shows on this day. Try another date.</div>}
      {byTheater.map((t) => (
        <div key={t.name} className="card border-0 shadow-sm mb-3">
          <div className="card-body">
            <h6 className="mb-0">{t.name}</h6>
            <div className="small text-muted mb-3">📍 {t.address || t.city}</div>
            <div className="d-flex flex-wrap gap-2">
              {t.shows.map((s) => (
                <Link key={s.id} to={`/shows/${s.id}/seats`} className={`btn time-btn ${s.availableSeats === 0 ? 'btn-outline-secondary disabled' : s.availableSeats < s.capacity * 0.2 ? 'btn-outline-warning' : 'btn-outline-success'}`}>
                  <div className="fw-semibold">{formatTime(s.startTime)}</div>
                  <div className="small">{s.availableSeats === 0 ? 'Sold out' : `${formatPrice(s.price)}+ · ${s.availableSeats} left`}</div>
                </Link>
              ))}
            </div>
          </div>
        </div>
      ))}
    </>
  );
}
