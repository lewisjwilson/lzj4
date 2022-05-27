%===============================LZ4 Decoder=====================================
%=========================Max Block/File size 64KB==============================


function lz4_decoder_final(fname)

%Read file into an array
fid = fopen(fname, 'r');
data = fread(fid, 'uint8');
fclose(fid);

pos = 1;

tic

%Checking the file is a valid LZ4 file
if(data(1:4)==[4 34 77 24]')
  %LZ4 File Detected
  %Skip first 7 bytes...
  pos = 12;
else
  fprintf("Invalid LZ4 File\n")
  return
end


len = length(data);
decoded_arr = cell();

%Data blocks
while(pos<=len)
  token = dec2hex(data(pos), 2); pos++;
  hi_token = hex2dec(substr(token,1,1));
  lo_token = hex2dec(substr(token, -1));
  
  if(hi_token==15)
    last_byte=255;
    while last_byte==255
      last_byte = data(pos);
      hi_token = hi_token + last_byte;
      pos++;
    end
  end
  
  %append the literals to the array
  if(hi_token!=0)
    for i=(pos:pos+hi_token-1)
        decoded_arr(end+1, 1) = char(data(i));
    end
    pos = pos+hi_token;
  end
  
  %2 Bytes Value (Little Endian)
  offset = data(pos)+256*data(pos+1);
  
  %Checking if the offset is zero
  if(offset==0)
    %Decoding Complete
    %Writing to file...
    break;
  end
  
  pos += 2;
  
  if(lo_token==15)
    last_byte=255;
    while last_byte==255
      last_byte = data(pos);
      lo_token = lo_token + last_byte;
      pos++;
    end
  end
  
  temp_len = length(decoded_arr);
  %Append 'lo_token' number of chars from the specified position
  for(i = 1:lo_token+4)
    decoded_arr(end+1, 1) = char(decoded_arr(temp_len-offset+i));
  end
  
end

toc

%Output to file
fid = fopen(strcat(fname, "_decoded.txt"), "w");
fprintf(fid, '%s', decoded_arr'{:})
fclose(fid);

endfunction