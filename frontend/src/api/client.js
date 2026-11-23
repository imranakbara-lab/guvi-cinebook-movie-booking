import axios from 'axios';

export const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080').replace(/\/$/, '');

const api = axios.create({ baseURL: `${API_URL}/api` });

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && localStorage.getItem('token')) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);

export function errorMessage(err) {
  const data = err?.response?.data;
  if (data?.fieldErrors) return Object.values(data.fieldErrors).join(', ');
  return data?.message || err?.message || 'Something went wrong';
}

const d = (r) => r.data;
const clean = (params) => Object.fromEntries(Object.entries(params || {}).filter(([, v]) => v !== '' && v !== null && v !== undefined));

export const authApi = {
  login: (body) => api.post('/auth/login', body).then(d),
  register: (body) => api.post('/auth/register', body).then(d),
};

export const catalogApi = {
  movies: (params) => api.get('/movies', { params: clean(params) }).then(d),
  movie: (id) => api.get(`/movies/${id}`).then(d),
  showsForMovie: (id, params) => api.get(`/movies/${id}/shows`, { params: clean(params) }).then(d),
  genres: () => api.get('/genres').then(d),
  cities: () => api.get('/cities').then(d),
  theaters: () => api.get('/theaters').then(d),
  show: (id) => api.get(`/shows/${id}`).then(d),
  seats: (id) => api.get(`/shows/${id}/seats`).then(d),
};

export const bookingApi = {
  hold: (showId, seats) => api.post('/bookings', { showId, seats }).then(d),
  paymentOrder: (id) => api.post(`/bookings/${id}/payment-order`).then(d),
  confirm: (id, body) => api.post(`/bookings/${id}/payment-confirm`, body || {}).then(d),
  my: () => api.get('/bookings/my').then(d),
  get: (id) => api.get(`/bookings/${id}`).then(d),
  cancel: (id) => api.post(`/bookings/${id}/cancel`).then(d),
};

export const profileApi = {
  get: () => api.get('/users/me').then(d),
  update: (body) => api.put('/users/me', body).then(d),
  changePassword: (body) => api.put('/users/me/password', body).then(d),
};

export const adminApi = {
  movies: () => api.get('/admin/movies').then(d),
  createMovie: (b) => api.post('/admin/movies', b).then(d),
  updateMovie: (id, b) => api.put(`/admin/movies/${id}`, b).then(d),
  deleteMovie: (id) => api.delete(`/admin/movies/${id}`),
  createTheater: (b) => api.post('/admin/theaters', b).then(d),
  updateTheater: (id, b) => api.put(`/admin/theaters/${id}`, b).then(d),
  deleteTheater: (id) => api.delete(`/admin/theaters/${id}`),
  shows: (upcoming = true) => api.get('/admin/shows', { params: { upcoming } }).then(d),
  createShow: (b) => api.post('/admin/shows', b).then(d),
  updateShow: (id, b) => api.put(`/admin/shows/${id}`, b).then(d),
  deleteShow: (id) => api.delete(`/admin/shows/${id}`),
  bookings: (showId) => api.get('/admin/bookings', { params: clean({ showId }) }).then(d),
  cancelBooking: (id) => api.post(`/admin/bookings/${id}/cancel`).then(d),
  users: () => api.get('/admin/users').then(d),
  updateUser: (id, b) => api.put(`/admin/users/${id}`, b).then(d),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
};

export default api;
