import { Injectable } from '@angular/core';

const FLASH_MESSAGE_KEY = 'flash_message';

@Injectable({
  providedIn: 'root'
})
export class FlashMessageService {

  set(message: string): void {
    sessionStorage.setItem(FLASH_MESSAGE_KEY, message);
  }

  consume(): string | null {
    const message = sessionStorage.getItem(FLASH_MESSAGE_KEY);
    if (null !== message) {
      sessionStorage.removeItem(FLASH_MESSAGE_KEY);
    }
    return message
  }
}
