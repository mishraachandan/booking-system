import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Movie } from '../../core/services/show.service';
import { MovieService } from '../../core/services/movie.service';
import { ShowService, Show } from '../../core/services/show.service';

@Component({
  selector: 'app-movie-detail',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="movie-detail-page page-enter">
      @if (loading) {
        <div class="loading-state"><div class="spinner"></div><p>Loading movie...</p></div>
      } @else if (error) {
        <div class="error-box">
          <p>{{ error }}</p>
          <a routerLink="/" class="btn btn-outline">Back to Home</a>
        </div>
      } @else if (movie) {
        <!-- Hero banner -->
        <section class="hero-banner" [style.background-image]="'url(' + movie.posterUrl + ')'">
          <div class="hero-overlay"></div>
          <div class="container hero-content">
            <div class="poster-wrap">
              <img [src]="movie.posterUrl || fallbackPoster" [alt]="movie.title" class="poster" />
            </div>
            <div class="hero-info">
              <h1 class="title">{{ movie.title }}</h1>
              <div class="meta-row">
                @if (movie.rating) {
                  <span class="rating">★ {{ movie.rating }}/10</span>
                }
                <span class="chip">{{ movie.language }}</span>
                <span class="chip">{{ movie.genre }}</span>
                <span class="chip">{{ movie.durationMinutes }} min</span>
                @if (movie.releaseDate) {
                  <span class="chip">{{ movie.releaseDate | date:'mediumDate' }}</span>
                }
              </div>
              <p class="synopsis">{{ movie.description }}</p>
              <div class="actions">
                <button class="btn btn-primary" (click)="scrollToShows()" [disabled]="showsLoading">
                  🎟 Book Tickets
                </button>
                @if (trailerSafeUrl) {
                  <button class="btn btn-outline" (click)="toggleTrailer()">
                    {{ trailerOpen ? '✕ Close Trailer' : '▶ Watch Trailer' }}
                  </button>
                }
              </div>
            </div>
          </div>
        </section>

        <!-- Trailer -->
        @if (trailerOpen && trailerSafeUrl) {
          <section class="trailer-section container">
            <div class="trailer-wrap">
              <iframe
                [src]="trailerSafeUrl"
                frameborder="0"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
              </iframe>
            </div>
          </section>
        }

        <!-- Cast -->
        @if (movie.cast && movie.cast.length > 0) {
          <section class="cast-section container">
            <h2>Cast</h2>
            <div class="cast-list">
              @for (actor of movie.cast; track actor) {
                <div class="cast-chip">
                  <div class="cast-avatar">{{ initialsOf(actor) }}</div>
                  <span>{{ actor }}</span>
                </div>
              }
            </div>
          </section>
        }

        <!-- Showtimes -->
        <section class="shows-section container" id="shows">
          <h2>Showtimes</h2>
          @if (showsLoading) {
            <p class="text-muted">Loading showtimes...</p>
          } @else if (shows.length === 0) {
            <div class="empty-state">
              <p>No showtimes available for this movie right now.</p>
            </div>
          } @else {
            <div class="shows-grid">
              @for (show of shows; track show.id) {
                <a [routerLink]="['/show', show.id, 'seats']" class="card show-card">
                  <div class="show-cinema">{{ show.screen?.cinema?.name || 'Cinema' }}</div>
                  <div class="show-screen">{{ show.screen?.name || 'Screen' }}</div>
                  <div class="show-time">{{ show.startTime | date:'hh:mm a' }}</div>
                  <div class="show-date">{{ show.startTime | date:'MMM d, yyyy' }}</div>
                </a>
              }
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .movie-detail-page { padding-bottom: 80px; }
    .loading-state, .error-box {
      text-align: center; padding: 120px 20px; color: var(--text-muted);
      p { margin: 16px 0; }
    }
    .spinner {
      width: 48px; height: 48px; border: 3px solid var(--border);
      border-top-color: var(--accent); border-radius: 50%;
      animation: spin 0.8s linear infinite; margin: 0 auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .hero-banner {
      position: relative;
      background-size: cover; background-position: center;
      padding: 60px 0 40px;
    }
    .hero-overlay {
      position: absolute; inset: 0;
      background: linear-gradient(135deg, rgba(10, 11, 20, 0.92), rgba(10, 11, 20, 0.72));
      backdrop-filter: blur(6px);
    }
    .hero-content {
      position: relative;
      display: flex; gap: 32px; align-items: flex-start;
    }
    .poster-wrap {
      flex: 0 0 240px;
      .poster {
        width: 100%; aspect-ratio: 2 / 3; object-fit: cover;
        border-radius: var(--radius); box-shadow: 0 12px 40px rgba(0,0,0,0.5);
      }
    }
    .hero-info { flex: 1; min-width: 0; }
    .title { font-size: 36px; font-weight: 800; margin-bottom: 12px; }
    .meta-row {
      display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 16px;
    }
    .rating {
      background: rgba(241, 196, 15, 0.18); color: #f1c40f;
      padding: 4px 12px; border-radius: 20px;
      font-weight: 700; font-size: 13px;
    }
    .chip {
      background: rgba(255,255,255,0.08); color: var(--text-secondary);
      padding: 4px 12px; border-radius: 20px; font-size: 12px;
    }
    .synopsis {
      color: var(--text-secondary); font-size: 15px; line-height: 1.55;
      max-width: 720px; margin-bottom: 24px;
    }
    .actions { display: flex; gap: 12px; flex-wrap: wrap; }

    .trailer-section { padding: 32px 20px; }
    .trailer-wrap {
      position: relative; aspect-ratio: 16 / 9; max-width: 960px; margin: 0 auto;
      border-radius: var(--radius); overflow: hidden; background: #000;
    }
    .trailer-wrap iframe {
      position: absolute; inset: 0; width: 100%; height: 100%;
    }

    .cast-section, .shows-section {
      padding: 32px 20px;
      h2 { font-size: 22px; font-weight: 700; margin-bottom: 16px; }
    }
    .cast-list { display: flex; flex-wrap: wrap; gap: 12px; }
    .cast-chip {
      display: flex; align-items: center; gap: 10px;
      background: var(--bg-card); border: 1px solid var(--border);
      border-radius: 999px; padding: 6px 14px 6px 6px;
      font-size: 14px;
    }
    .cast-avatar {
      width: 32px; height: 32px; border-radius: 50%;
      background: var(--accent-gradient); color: white;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 12px;
    }

    .shows-grid {
      display: grid; gap: 12px;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    }
    .show-card {
      padding: 16px; transition: transform 0.15s, border-color 0.15s;
      text-decoration: none; color: inherit;
      &:hover { transform: translateY(-2px); border-color: var(--accent); }
    }
    .show-cinema { font-weight: 600; font-size: 14px; margin-bottom: 2px; }
    .show-screen { font-size: 12px; color: var(--text-muted); margin-bottom: 8px; }
    .show-time { font-size: 20px; font-weight: 700; color: var(--accent); }
    .show-date { font-size: 12px; color: var(--text-muted); margin-top: 2px; }

    .empty-state { text-align: center; padding: 40px 20px; color: var(--text-muted); }

    @media (max-width: 768px) {
      .hero-content { flex-direction: column; gap: 20px; }
      .poster-wrap { flex: 0 0 auto; max-width: 200px; margin: 0 auto; }
      .title { font-size: 26px; text-align: center; }
      .meta-row { justify-content: center; }
      .synopsis { text-align: center; }
      .actions { justify-content: center; }
    }
  `]
})
export class MovieDetailComponent implements OnInit {
  movie: Movie | null = null;
  shows: Show[] = [];
  loading = true;
  showsLoading = true;
  error = '';
  trailerOpen = false;
  trailerSafeUrl: SafeResourceUrl | null = null;
  fallbackPoster = 'https://placehold.co/300x450/1a1a2e/e23744?text=Movie';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private movieService: MovieService,
    private showService: ShowService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const movieId = Number(this.route.snapshot.paramMap.get('movieId'));
    if (!movieId) {
      this.error = 'Invalid movie id.';
      this.loading = false;
      return;
    }
    this.movieService.getMovie(movieId).subscribe({
      next: (m) => {
        this.movie = m;
        if (m.trailerUrl) {
          this.trailerSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(m.trailerUrl);
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'Movie not found.';
        this.loading = false;
      }
    });

    this.showService.getShowsByMovie(movieId).subscribe({
      next: (s) => {
        // Only future shows
        const now = Date.now();
        this.shows = s.filter(x => new Date(x.startTime).getTime() > now)
          .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
        this.showsLoading = false;
      },
      error: () => { this.showsLoading = false; }
    });
  }

  toggleTrailer() { this.trailerOpen = !this.trailerOpen; }

  scrollToShows() {
    setTimeout(() => document.getElementById('shows')?.scrollIntoView({ behavior: 'smooth' }), 50);
  }

  initialsOf(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]).join('').toUpperCase();
  }
}
