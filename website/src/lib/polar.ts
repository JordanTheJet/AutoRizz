import { Polar } from "@polar-sh/sdk";

let _polar: Polar | null = null;

export function getPolar(): Polar {
  if (!_polar) {
    _polar = new Polar({ accessToken: process.env.POLAR_ACCESS_TOKEN! });
  }
  return _polar;
}
