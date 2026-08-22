# Domain Ownership

  Domain                  Owner
  ----------------------- ---------
  Identity                Backend
  Salon                   Backend
  Tenant Data             Backend
  Membership              Backend
  Permission              Backend
  Media                   Backend
  Booking                 Backend
  Calendar / Availability Backend
  Customer Relationship   Backend

قانون: ایجاد مدل تجاری مستقل در Client ممنوع است.

Calendar و Availability بخشی از Booking Domain هستند، نه یک Domain
مستقل در هیچ Client — جزئیات در
03_APPLICATION_ARCHITECTURE/APPLICATION_RULES.md (Desktop) و
10_ARCHITECTURE_DECISIONS_ADR/ADR-004_BOOKING_MUTATION_RELIABILITY.md.
