import { Component } from '@angular/core';
import { TypographyComponent } from '../../shared/components/typography/typography.component';
import { ButtonComponent } from '../../shared/components/button/button.component';
import { SectionComponent } from '../../shared/components/section/section.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [TypographyComponent, ButtonComponent, SectionComponent, RouterModule],
  templateUrl: './not-found.html',
  styleUrl: './not-found.css',
})
export class NotFound {

}
