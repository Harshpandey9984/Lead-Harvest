# TODO

## Social/Phone/Email/Address extraction fix
- [x] Update extraction to capture `href`/`tel:`/`mailto:` for phone/email and social links

- [ ] Add support for extracting multiple social links (not just `selectFirst`)
- [x] Improve address extraction by joining structured nodes when a container selector is used
- [ ] Ensure payload keys are stable (twitter, facebook, instagram, linkedin, phone, email, address)
- [ ] Add normalization rules for phone/email formatting
- [x] Build + run tests
- [ ] Validate by re-running scraper and checking exported Excel

