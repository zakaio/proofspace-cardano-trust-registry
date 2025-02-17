import EN from '../../locales/en.json';
import AR from '../../locales/ar.json';
import DE from '../../locales/de.json';
import UA from '../../locales/ua.json';


/*const EN = {};
const AR = {};
const DE = {};
const UA = {};*/

export interface Locale {
  [key: string]: string;
}

class LocalesManager {
  private localesSet = new Map<string, Locale>();
  private currentLocale: Locale = EN;
  private currentKey = 'EN';

  init() {
    this.localesSet.set('EN', EN);
    this.localesSet.set('DE', DE);
    this.localesSet.set('UA', UA);
    this.localesSet.set('AR', AR);
    this.currentKey = localStorage.getItem('language') || 'EN';
    this.currentLocale = this.localesSet.get(this.currentKey) || EN;
  }

  localize(key: string, ...params: any[]) {
    let str = this.currentLocale[key] || key;
    if (params?.length) {
      params.forEach((p, i) => {
        str = str.replace(`{${i}}`, p);
      });
    }
    return str;
  }

  setLocale(locale: string) {
    localStorage.setItem('language', locale);

    this.currentKey = locale;
    this.currentLocale = this.localesSet.get(this.currentKey) || UA;
  }

  get locale() {
    return this.currentLocale;
  }

  get key() {
    return this.currentKey;
  }
}

export const localesManager = new LocalesManager();

export const setLocale = (locale: string) => localesManager.setLocale(locale);

export const localize = (key: string, ...params: any[]) => {
  return localesManager.localize(key, ...params);
};
