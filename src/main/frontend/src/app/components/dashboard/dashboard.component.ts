import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface FileStats {
  totalFiles: number;
  totalSize: number;
  recentUpload: string | null;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
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

  constructor(private http: HttpClient) {}

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
      this.uploadFiles(files);
    }
  }

  onFileSelect(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.uploadFiles(input.files);
    }
  }

  uploadFiles(files: FileList): void {
    this.isUploading = true;

    Array.from(files).forEach(file => {
      const reader = new FileReader();
      reader.onload = () => {
        const content = reader.result as string;

        const payload = {
          id: `doc-${Date.now()}-${file.name}`,
          text: content,
          metadata: {
            filename: file.name,
            size: file.size,
            type: file.type,
            uploadedAt: new Date().toISOString()
          }
        };

        this.http.post('/api/documents/upload', payload).subscribe({
          next: () => {
            this.uploadedFiles.push(file.name);
            this.stats.totalFiles++;
            this.stats.totalSize += file.size;
            this.stats.recentUpload = file.name;
            this.isUploading = false;
          },
          error: (error) => {
            console.error('Upload failed:', error);
            this.isUploading = false;
          }
        });
      };
      reader.readAsText(file);
    });
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }
}