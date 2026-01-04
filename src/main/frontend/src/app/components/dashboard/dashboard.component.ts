import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

interface FileStats {
  totalFiles: number;
  totalSize: number;
  recentUpload: string | null;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  stats: FileStats = {
    totalFiles: 0,
    totalSize: 0,
    recentUpload: null
  };

  isDragging = false;
  isUploading = false;
  uploadedFiles: string[] = [];
  uploadError: string | null = null;

  // URL Crawl
  crawlUrl = '';
  isCrawling = false;
  crawlError: string | null = null;
  crawlSuccess: string | null = null;
  crawledUrls: string[] = [];

  private readonly allowedExtensions = [
    '.pdf', '.doc', '.docx',  // Documents
    '.txt', '.rtf', '.md',    // Text
    '.csv', '.json', '.xml',  // Data
    '.html', '.htm',          // Web
    '.xls', '.xlsx',          // Excel
    '.ppt', '.pptx',          // PowerPoint
    '.odt', '.ods', '.odp'    // OpenDocument
  ];

  constructor(private http: HttpClient) {}

  private isAllowedFile(file: File): boolean {
    const extension = '.' + file.name.split('.').pop()?.toLowerCase();
    return this.allowedExtensions.includes(extension);
  }

  private filterAllowedFiles(files: FileList): File[] {
    const allowedFiles: File[] = [];
    const rejectedFiles: string[] = [];

    Array.from(files).forEach(file => {
      if (this.isAllowedFile(file)) {
        allowedFiles.push(file);
      } else {
        rejectedFiles.push(file.name);
      }
    });

    if (rejectedFiles.length > 0) {
      this.uploadError = `Rejected files (unsupported format): ${rejectedFiles.join(', ')}`;
      setTimeout(() => this.uploadError = null, 5000);
    }

    return allowedFiles;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragging = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      const allowedFiles = this.filterAllowedFiles(files);
      if (allowedFiles.length > 0) {
        this.uploadFilteredFiles(allowedFiles);
      }
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const allowedFiles = this.filterAllowedFiles(input.files);
      if (allowedFiles.length > 0) {
        this.uploadFilteredFiles(allowedFiles);
      }
    }
  }

  uploadFilteredFiles(files: File[]): void {
    this.isUploading = true;
    let completedUploads = 0;

    files.forEach(file => {
      const formData = new FormData();
      formData.append('file', file);

      this.http.post<{success: boolean; message: string; filename: string}>('/api/documents/upload-file', formData).subscribe({
        next: (response) => {
          if (response.success) {
            this.uploadedFiles.push(file.name);
            this.stats.totalFiles++;
            this.stats.totalSize += file.size;
            this.stats.recentUpload = file.name;
          } else {
            this.uploadError = `Failed to upload ${file.name}: ${response.message}`;
            setTimeout(() => this.uploadError = null, 5000);
          }
          completedUploads++;
          if (completedUploads === files.length) {
            this.isUploading = false;
          }
        },
        error: (error) => {
          console.error('Upload failed:', error);
          this.uploadError = `Failed to upload ${file.name}: ${error.error?.message || 'Unknown error'}`;
          setTimeout(() => this.uploadError = null, 5000);
          completedUploads++;
          if (completedUploads === files.length) {
            this.isUploading = false;
          }
        }
      });
    });
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  onCrawlUrl(): void {
    if (!this.crawlUrl || this.isCrawling) return;

    this.isCrawling = true;
    this.crawlError = null;
    this.crawlSuccess = null;

    this.http.post<{success: boolean; message: string; url: string; title: string; chunks: number}>('/api/documents/crawl', {
      url: this.crawlUrl,
      maxPages: 1
    }).subscribe({
      next: (response) => {
        if (response.success) {
          this.crawledUrls.push(response.title || response.url);
          this.crawlSuccess = `Successfully indexed "${response.title}" (${response.chunks} chunks)`;
          this.stats.totalFiles++;
          this.stats.recentUpload = response.title || response.url;
          this.crawlUrl = '';
          setTimeout(() => this.crawlSuccess = null, 5000);
        } else {
          this.crawlError = response.message;
          setTimeout(() => this.crawlError = null, 5000);
        }
        this.isCrawling = false;
      },
      error: (error) => {
        console.error('Crawl failed:', error);
        this.crawlError = error.error?.message || 'Failed to crawl URL';
        setTimeout(() => this.crawlError = null, 5000);
        this.isCrawling = false;
      }
    });
  }
}