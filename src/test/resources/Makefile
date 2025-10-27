.PHONY = permissions archives compress clean

AR  = archive
ARS = archives
CPF = compress.txt
COM = compression

all: permissions archives compress

permissions:
	cd $(AR); \
	chmod 755 permissions/executable_file.txt; \
	chmod 700 permissions/private_executable_file.txt; \
	chmod 444 permissions/readonly_file.txt; \
	chmod 700 permissions/private_folder; \
	chmod 600 permissions/private_folder/private_file.txt;

## archives

archives: a_7z a_ar a_bz2 a_cpio a_jar a_tar a_tgz a_txz a_zip

a_7z:
	cd $(AR);                          \
	7zz a -t7z ../$(ARS)/$(AR).7z . > /dev/null;

a_ar:
	cd $(AR); \
	ar cr ../$(ARS)/$(AR).ar `find * -type f`;

a_bz2:
	cd $(AR);                 \
    COPYFILE_DISABLE=1; export COPYFILE_DISABLE; \
	tar cjf ../$(ARS)/$(AR).tar.bz2 *;

a_cpio:
	cd $(AR);                                                         \
	find . -depth ! -path . -print | sed 's|^\./||' | cpio --quiet -o > ../$(ARS)/$(AR).cpio;

a_jar:
	cd $(AR);             \
	jar cMf ../$(ARS)/$(AR).jar .;

a_tar:
	cd $(AR);           \
    COPYFILE_DISABLE=1; export COPYFILE_DISABLE; \
	tar cf ../$(ARS)/$(AR).tar *;

a_tgz:
	cd $(AR);                \
    COPYFILE_DISABLE=1; export COPYFILE_DISABLE; \
	tar czf ../$(ARS)/$(AR).tar.gz *;

a_txz:
	cd $(AR);               \
    COPYFILE_DISABLE=1; export COPYFILE_DISABLE; \
	tar cJf ../$(ARS)/$(AR).tar.xz *;

a_zip:
	cd $(AR);           \
	zip -q -r ../$(ARS)/$(AR) .;

## compress
compress: c_bz2 c_gz c_lzma c_xz c_deflate

c_bz2:
	bzip2 -k $(CPF); \
    mv $(CPF).bz2 $(COM);

c_gz:
	gzip -k $(CPF) > $(COM)/$(CPF).gz; \
    mv $(CPF).gz $(COM);

c_lzma:
	lzma -z -k $(CPF) > $(COM)/$(CPF).lzma; \
    mv $(CPF).lzma $(COM);

c_xz:
	xz -z -k $(CPF) > $(COM)/$(CPF).xz; \
    mv $(CPF).xz $(COM);

c_deflate:
	python3 -c "import zlib; data = open('$(CPF)', 'rb').read(); open('$(COM)/$(CPF).deflate', 'wb').write(zlib.compress(data, 9))"

## clean
clean: clean-archives clean-compress

clean-archives:
	rm -f $(ARS)/(AR).*

clean-compress:
	rm -f $(COM)/$(CPF).*
